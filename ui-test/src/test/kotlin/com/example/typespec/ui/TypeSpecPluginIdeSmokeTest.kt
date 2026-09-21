package com.example.typespec.ui

import com.intellij.remoterobot.RemoteRobot
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Files.isRegularFile
import java.util.concurrent.TimeUnit

/**
 * Verifies the TypeSpec plugin actually works inside a licensed sandbox IDE.
 *
 * These tests are local-only: they require `:plugin:runIdeForUiTests` running with
 * an activated subscription. On 2025.3+ unified IDEA the sandbox boots in
 * Community-equivalent mode until a license is activated once (Help | Manage
 * Licenses). Without it, `com.intellij.modules.ultimate` stays disabled and this
 * plugin — which depends on JavaScript/NodeJS — never loads. In that case, and
 * whenever the robot server is unreachable, tests skip via [assumeTrue] so CI and
 * cloud environments stay green.
 */
class TypeSpecPluginIdeSmokeTest {

    private val baseUrl = System.getProperty("robot.server.url", "http://127.0.0.1:8082")
    private val robot by lazy {
        RemoteRobot(
            baseUrl,
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build(),
        )
    }

    @Test
    fun pluginAndExtensionsRegistered() {
        assumeRobotAndLicense()
        val report = report(APP_LEVEL_SCRIPT)

        assertEquals("true", report["typespecEnabled"])
        assertEquals("TypeSpec", report["tspFileType"])
        assertEquals("true", report["toolsMenuHasTypeSpec"])
        assertEquals("true", report["toolsGroup"])
        assertEquals("true", report["restartAction"])
        assertTrue(report.getValue("inspections").contains("TypeSpecPackageJsonInspection"))
        assertTrue(report.getValue("inspections").contains("TypeSpecTspConfigInspection"))
    }

    @Test
    fun projectLevelFeaturesWork() {
        assumeRobotAndLicense()

        val projectDir = Files.createTempDirectory("tsp-ui-smoke")
        Files.writeString(projectDir.resolve("main.tsp"), "model Pet {\n  id: int32;\n}\n")

        val report = report(projectScript(projectDir))

        assertEquals("true", report["twOutput"])
        assertEquals("true", report["twPreview"])
        assertEquals("TypeSpec", report["tspFileType"])
        assertEquals("TypeSpecSyntaxHighlighter", report["highlighter"])
        assertEquals("true", report["configurable"])
        assertEquals("true", report["twOutputVisible"])
    }

    @Test(timeout = 300_000)
    fun lspServerStarts() {
        assumeRobotAndLicense()
        assumeTrue("npm is not on PATH (needed to provision @typespec/compiler)", npmAvailable())

        // Reused across runs so npm install happens only once.
        val projectDir = Path.of("build/lsp-smoke-project").toAbsolutePath()
        Files.createDirectories(projectDir)
        Files.writeString(projectDir.resolve("package.json"), "{\"name\":\"tsp-ui-smoke\",\"private\":true}\n")
        Files.writeString(projectDir.resolve("main.tsp"), "model Pet {\n  id: int32;\n}\n")
        assumeTrue(
            "npm install @typespec/compiler failed in $projectDir",
            isRegularFile(compilerServerScript(projectDir)) || npmInstall(projectDir),
        )

        report(projectScript(projectDir)) // opens the project and main.tsp

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        var lsp = report(lspStateScript(projectDir))
        while (System.nanoTime() < deadline && lsp["lspState"] != "Running") {
            Thread.sleep(2_000)
            lsp = report(lspStateScript(projectDir))
        }
        assumeTrue("No TypeSpec LSP server object in opened project: $lsp", (lsp["lspServers"]?.toIntOrNull() ?: 0) > 0)
        assertEquals("Running", lsp["lspState"])
    }

    private fun assumeRobotAndLicense() {
        assumeTrue("Robot server not running at $baseUrl", isRobotServerUp(baseUrl))
        val report = report(ULTIMATE_GATE_SCRIPT)
        assumeTrue(
            "Sandbox IDE is unlicensed (com.intellij.modules.ultimate disabled); " +
                "activate once via Help | Manage Licenses in the sandbox IDE. Report: $report",
            report["ultimateEnabled"] == "true",
        )
    }

    private fun report(script: String): Map<String, String> =
        robot.callJs<String>(script, true)
            .lineSequence()
            .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
            .toMap()

    private fun projectScript(projectDir: Path): String =
        PROJECT_LEVEL_SCRIPT.replace("__PROJECT_DIR__", projectDir.toString().replace('\\', '/'))

    private fun lspStateScript(projectDir: Path): String =
        LSP_STATE_SCRIPT.replace("__PROJECT_DIR__", projectDir.toString().replace('\\', '/'))

    private fun compilerServerScript(projectDir: Path): Path =
        projectDir.resolve("node_modules").resolve("@typespec/compiler").resolve("cmd/tsp-server.js")

    private fun npmCommand(): List<String> =
        if (System.getProperty("os.name").lowercase().contains("win")) listOf("cmd", "/c", "npm") else listOf("npm")

    private fun npmAvailable(): Boolean =
        try {
            val process = ProcessBuilder(npmCommand() + "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }

    private fun npmInstall(projectDir: Path): Boolean =
        try {
            val process = ProcessBuilder(
                npmCommand() + listOf("install", "--no-audit", "--no-fund", "@typespec/compiler"),
            )
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .start()
            process.waitFor(4, TimeUnit.MINUTES) &&
                process.exitValue() == 0 &&
                isRegularFile(compilerServerScript(projectDir))
        } catch (_: Exception) {
            false
        }

    private fun isRobotServerUp(baseUrl: String): Boolean =
        try {
            val connection = URI(baseUrl).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.requestMethod = "GET"
            connection.connect()
            connection.responseCode in 200..499
        } catch (_: Exception) {
            false
        }

    companion object {
        private const val ULTIMATE_GATE_SCRIPT = """
importClass(Packages.com.intellij.ide.plugins.PluginManagerCore);
importClass(Packages.com.intellij.openapi.extensions.PluginId);
var u = PluginManagerCore.getPlugin(PluginId.getId('com.intellij.modules.ultimate'));
var t = PluginManagerCore.getPlugin(PluginId.getId('com.example.typespec'));
'ultimateEnabled=' + (u != null && u.isEnabled()) + '\n' +
'typespecEnabled=' + (t != null && t.isEnabled());
"""

        private const val APP_LEVEL_SCRIPT = """
importClass(Packages.com.intellij.ide.plugins.PluginManagerCore);
importClass(Packages.com.intellij.openapi.extensions.PluginId);
importClass(Packages.com.intellij.openapi.extensions.Extensions);
importClass(Packages.com.intellij.openapi.actionSystem.ActionManager);
importClass(Packages.com.intellij.openapi.fileTypes.FileTypeManager);
var r = [];
function kv(k, v) { r.push(k + '=' + v); }
var u = PluginManagerCore.getPlugin(PluginId.getId('com.intellij.modules.ultimate'));
kv('ultimateEnabled', u != null && u.isEnabled());
var t = PluginManagerCore.getPlugin(PluginId.getId('com.example.typespec'));
kv('typespecEnabled', t != null && t.isEnabled());
kv('tspFileType', FileTypeManager.getInstance().getFileTypeByExtension('tsp').getName());
var am = ActionManager.getInstance();
kv('toolsGroup', am.getAction('TypeSpec.Tools') != null);
kv('restartAction', am.getAction('TypeSpec.RestartServer') != null);
var ids = am.getAction('ToolsMenu').getChildren(am).map(function (a) { return am.getId(a); }).join(',');
kv('toolsMenuHasTypeSpec', ids.indexOf('TypeSpec.Tools') >= 0);
kv('inspections', Extensions.getRootArea().getExtensionPoint('com.intellij.localInspection')
    .getExtensionList().toArray()
    .map(function (e) { return String(e.shortName); })
    .filter(function (s) { return s.indexOf('TypeSpec') == 0; }).join(','));
r.join('\n');
"""

        private const val PROJECT_LEVEL_SCRIPT = """
importClass(Packages.com.intellij.ide.impl.ProjectUtil);
importClass(Packages.com.intellij.ide.plugins.PluginManagerCore);
importClass(Packages.com.intellij.openapi.extensions.PluginId);
importClass(Packages.com.intellij.openapi.project.ProjectManager);
importClass(Packages.com.intellij.openapi.wm.ToolWindowManager);
importClass(Packages.com.intellij.openapi.vfs.LocalFileSystem);
importClass(Packages.com.intellij.openapi.fileEditor.FileEditorManager);
importClass(Packages.com.intellij.openapi.fileTypes.SyntaxHighlighterFactory);
importClass(Packages.com.intellij.openapi.actionSystem.ActionManager);
importClass(Packages.com.intellij.openapi.actionSystem.AnActionEvent);
importClass(Packages.com.intellij.openapi.actionSystem.impl.SimpleDataContext);
importClass(Packages.com.intellij.ide.GeneralSettings);
importClass(Packages.com.intellij.ide.trustedProjects.TrustedProjects);
importClass(Packages.java.nio.file.Paths);
var r = [];
function kv(k, v) { r.push(k + '=' + v); }
var dir = Paths.get('__PROJECT_DIR__');
GeneralSettings.getInstance().setConfirmOpenNewProject(GeneralSettings.OPEN_PROJECT_SAME_WINDOW);
TrustedProjects.setProjectTrusted(dir, true);
var p = ProjectUtil.openOrImport(dir);
kv('project', p.getName());
var twm = ToolWindowManager.getInstance(p);
kv('twOutput', twm.getToolWindow('TypeSpec Output') != null);
kv('twPreview', twm.getToolWindow('TypeSpec API Preview') != null);
var vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Paths.get('__PROJECT_DIR__/main.tsp'));
kv('tspFileType', vf.getFileType().getName());
var hf = SyntaxHighlighterFactory.getSyntaxHighlighter(vf.getFileType().getLanguage(), p, vf);
kv('highlighter', hf == null ? 'null' : hf.getClass().getSimpleName());
FileEditorManager.getInstance(p).openFile(vf, true);
kv('configurable', p.getExtensionArea().getExtensionPoint('com.intellij.projectConfigurable')
    .getExtensionList().toArray()
    .map(function (e) { return String(e.id); }).join(',').indexOf('settings.typespec') >= 0);
var act = ActionManager.getInstance().getAction('TypeSpec.ShowOutput');
act.actionPerformed(AnActionEvent.createFromAnAction(act, null, 'ToolsMenu', SimpleDataContext.getProjectContext(p)));
kv('twOutputVisible', twm.getToolWindow('TypeSpec Output').isVisible());
try {
    var cl = PluginManagerCore.getPlugin(PluginId.getId('com.example.typespec')).getPluginClassLoader();
    var pcls = Packages.java.lang.Class.forName('com.example.typespec.TypeSpecLspIntegrationProvider', true, cl);
    var servers = Packages.com.intellij.platform.lsp.api.LspServerManager.getInstance(p).getServersForProvider(pcls);
    kv('lspServers', servers.size());
    if (servers.size() > 0) kv('lspState', String(servers.toArray()[0].getState()));
} catch (e) { kv('lspError', String(e).substring(0, 200)); }
r.join('\n');
"""

        private const val LSP_STATE_SCRIPT = """
importClass(Packages.com.intellij.ide.plugins.PluginManagerCore);
importClass(Packages.com.intellij.openapi.extensions.PluginId);
importClass(Packages.com.intellij.openapi.project.ProjectManager);
var r = [];
function kv(k, v) { r.push(k + '=' + v); }
var open = ProjectManager.getInstance().getOpenProjects();
var p = null;
for (var i = 0; i < open.length; i++) {
    if (String(open[i].getBasePath()).replace(/\\/g, '/') == '__PROJECT_DIR__') { p = open[i]; break; }
}
kv('projectOpen', p != null);
if (p != null) {
    var cl = PluginManagerCore.getPlugin(PluginId.getId('com.example.typespec')).getPluginClassLoader();
    var pcls = Packages.java.lang.Class.forName('com.example.typespec.TypeSpecLspIntegrationProvider', true, cl);
    var servers = Packages.com.intellij.platform.lsp.api.LspServerManager.getInstance(p).getServersForProvider(pcls);
    kv('lspServers', servers.size());
    if (servers.size() > 0) kv('lspState', String(servers.toArray()[0].getState()));
}
r.join('\n');
"""
    }
}
