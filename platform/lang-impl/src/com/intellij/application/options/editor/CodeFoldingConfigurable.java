// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.editor;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurableWithOptionDescriptors;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author yole
 */
public class CodeFoldingConfigurable extends CompositeConfigurable<CodeFoldingOptionsProvider> implements EditorOptionsProvider {
  public static final String ID = "editor.preferences.folding";
  private JCheckBox myCbFolding;
  private JPanel myRootPanel;
  private JPanel myFoldingPanel;

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("group.code.folding");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.folding";
  }

  @Override
  public JComponent createComponent() {
    myFoldingPanel.removeAll();
    for (CodeFoldingOptionsProvider provider : getConfigurables()) {
      myFoldingPanel
        .add(provider.createComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                                GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0));
    }
    return myRootPanel;
  }

  @NotNull
  List<OptionDescription> getDescriptors() {
    String byDefault = ApplicationBundle.message("label.fold.by.default");
    return JBIterable.from(getConfigurables())
      .map(c -> c instanceof ConfigurableWrapper ? ((ConfigurableWrapper)c).getConfigurable() : c)
      .filter(ConfigurableWithOptionDescriptors.class)
      .flatMap(c -> {
        String title = c instanceof BeanConfigurable ? ((BeanConfigurable<?>)c).getTitle() : null;
        String prefix = title == null ? byDefault + " " : StringUtil.trimEnd(byDefault, ':') + " in " + title + ": ";
        return c.getOptionDescriptors(ID, s -> prefix + s);
      }).toList();
  }

  @Override
  public boolean isModified() {
    return myCbFolding.isSelected() != EditorSettingsExternalizable.getInstance().isFoldingOutlineShown() ||
           super.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable.getInstance().setFoldingOutlineShown(myCbFolding.isSelected());
    super.apply();

    ApplicationManager.getApplication().invokeLater(() -> applyCodeFoldingSettingsChanges(), ModalityState.NON_MODAL);
  }

  public static void applyCodeFoldingSettingsChanges() {
    EditorOptionsPanel.reinitAllEditors();
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      Project project = editor.getProject();
      if (project != null && !project.isDefault()) CodeFoldingManager.getInstance(project).scheduleAsyncFoldingUpdate(editor);
    }
    ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorOptionsListener.FOLDING_CONFIGURABLE_TOPIC).changesApplied();
  }

  @Override
  public void reset() {
    myCbFolding.setSelected(EditorSettingsExternalizable.getInstance().isFoldingOutlineShown());
    super.reset();
  }

  @NotNull
  @Override
  protected List<CodeFoldingOptionsProvider> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(CodeFoldingOptionsProviderEP.EP_NAME);
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }
}
