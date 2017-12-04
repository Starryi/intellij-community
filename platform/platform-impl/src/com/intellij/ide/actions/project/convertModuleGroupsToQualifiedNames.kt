// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.project

import com.intellij.CommonBundle
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionProfileWrapper
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.lang.StdLanguages
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil
import java.awt.Color
import java.awt.Font
import java.util.function.Function
import java.util.function.Supplier
import javax.swing.Action
import javax.swing.JPanel

class ConvertModuleGroupsToQualifiedNamesDialog(val project: Project) : DialogWrapper(project) {
  private val editorArea: EditorTextField
  private val document: Document
    get() = editorArea.document
  private val modules = ModuleManager.getInstance(project).modules

  init {
    title = ProjectBundle.message("convert.module.groups.dialog.title")
    setOKButtonText(ProjectBundle.message("convert.module.groups.button.text"))
    editorArea = EditorTextFieldProvider.getInstance().getEditorField(StdLanguages.TEXT, project, listOf(EditorCustomization {
      it.settings.apply {
        isLineNumbersShown = false
        isLineMarkerAreaShown = false
        isFoldingOutlineShown = false
        isRightMarginShown = false
        additionalLinesCount = 0
        additionalColumnsCount = 0
        isAdditionalPageAtBottom = false
        isShowIntentionBulb = false
      }
      (it as? EditorImpl)?.registerLineExtensionPainter(this::generateLineExtension)
      setupHighlighting(it)
    }, MonospaceEditorCustomization.getInstance()))
    editorArea.text = generateInitialText()
    init()
  }

  private fun setupHighlighting(editor: Editor) {
    editor.putUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY, false)
    val inspections = Supplier<List<InspectionToolWrapper<*, *>>> {
      listOf(LocalInspectionToolWrapper(ModuleNamesListInspection()))
    }
    val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
    file?.putUserData(InspectionProfileWrapper.CUSTOMIZATION_KEY, Function {
      val profile = InspectionProfileImpl("Module names", inspections, null)
      for (spellCheckingToolName in SpellCheckingEditorCustomizationProvider.getInstance().spellCheckingToolNames) {
        profile.getToolsOrNull(spellCheckingToolName, project)?.isEnabled = false
      }
      InspectionProfileWrapper(profile)
    })
  }

  override fun createCenterPanel(): JPanel {
    val text = XmlStringUtil.wrapInHtml(ProjectBundle.message("convert.module.groups.description.text"))
    return JBUI.Panels.simplePanel(0, UIUtil.DEFAULT_VGAP).addToCenter(editorArea).addToTop(JBLabel(text))
  }

  override fun getPreferredFocusedComponent() = editorArea.focusTarget

  private fun generateLineExtension(line: Int): Collection<LineExtensionInfo> {
    val lineText = document.charsSequence.subSequence(document.getLineStartOffset(line), document.getLineEndOffset(line)).toString()
    if (line !in modules.indices || modules[line].name == lineText) return emptyList()

    val name = LineExtensionInfo(" <- ${modules[line].name}", JBColor.GRAY, null, null, Font.PLAIN)
    val groupPath = ModuleManager.getInstance(project).getModuleGroupPath(modules[line])
    if (groupPath == null) {
      return listOf(name)
    }
    val group = LineExtensionInfo(groupPath.joinToString(separator = "/", prefix = " (", postfix = ")"), Color.GRAY, null, null, Font.PLAIN)
    return listOf(name, group)
  }

  private fun generateInitialText(): String {
    val moduleManager = ModuleManager.getInstance(project)
    return modules.joinToString("\n") {
      (moduleManager.getModuleGroupPath(it)?.let { it.joinToString(".") + "." } ?: "") + it.name
    }
  }

  fun importMapping(mapping: Map<String, String>) {
    runWriteAction {
      document.setText(modules.joinToString("\n") { mapping.getOrDefault(it.name, it.name) })
    }
  }

  fun getMapping(): Map<String, String>? {
    val lines = document.charsSequence.split('\n')
    if (lines.size != modules.size) return null

    return modules.withIndex().filter { lines[it.index] != it.value.name }.associateByTo(LinkedHashMap(), { it.value.name }, { lines[it.index] })
  }

  override fun doOKAction() {
    val mapping = getMapping()
    if (mapping == null) {
      Messages.showErrorDialog(project, "Incorrect mapping!", CommonBundle.getErrorTitle())
      return
    }

    if (mapping.isNotEmpty()) {
      val model = ModuleManager.getInstance(project).modifiableModel
      val byName = modules.associateBy { it.name }
      for (entry in mapping) {
        model.renameModule(byName[entry.key]!!, entry.value)
      }
      modules.forEach {
        model.setModuleGroupPath(it, null)
      }
      runWriteAction {
        model.commit()
      }
    }

    super.doOKAction()
  }

  override fun createActions(): Array<Action> {
    return arrayOf(okAction, SaveModuleNameMappingAction(this),
                   LoadModuleNameMappingAction(this), cancelAction)
  }
}

class ConvertModuleGroupsToQualifiedNamesAction : DumbAwareAction(ProjectBundle.message("convert.module.groups.action.text"), ProjectBundle.message("convert.module.groups.action.description"), null) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ConvertModuleGroupsToQualifiedNamesDialog(project).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project != null && ModuleManager.getInstance(e.project!!).hasModuleGroups()
  }
}

