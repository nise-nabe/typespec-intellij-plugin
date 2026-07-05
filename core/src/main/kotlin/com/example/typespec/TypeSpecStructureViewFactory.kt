package com.example.typespec

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiFile

class TypeSpecStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile.fileType != TypeSpecFileType) {
            return null
        }
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                TypeSpecStructureViewModel(psiFile)
        }
    }
}

private class TypeSpecStructureViewModel(
    psiFile: PsiFile,
) : com.intellij.ide.structureView.StructureViewModelBase(
    psiFile,
    TypeSpecFileStructureElement(psiFile),
),
    StructureViewModel.ElementInfoProvider {
    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element is TypeSpecDeclarationStructureElement
}

private class TypeSpecFileStructureElement(
    private val psiFile: PsiFile,
) : StructureViewTreeElement {
    override fun getValue(): Any = psiFile
    override fun navigate(requestFocus: Boolean) {
        OpenFileDescriptor(psiFile.project, psiFile.virtualFile).navigate(requestFocus)
    }
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = psiFile.name
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean) = psiFile.fileType.icon
    }
    override fun getChildren(): Array<StructureViewTreeElement> =
        TypeSpecStructureParser.parse(psiFile.text)
            .map { TypeSpecDeclarationStructureElement(it, psiFile) }
            .toTypedArray()
}

private class TypeSpecDeclarationStructureElement(
    private val node: TypeSpecStructureNode,
    private val psiFile: PsiFile,
) : StructureViewTreeElement {
    override fun getValue(): Any = node
    override fun navigate(requestFocus: Boolean) {
        OpenFileDescriptor(psiFile.project, psiFile.virtualFile).navigate(requestFocus)
    }
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = "${node.kind} ${node.name}"
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean) = psiFile.fileType.icon
    }
    override fun getChildren(): Array<StructureViewTreeElement> = emptyArray()
}
