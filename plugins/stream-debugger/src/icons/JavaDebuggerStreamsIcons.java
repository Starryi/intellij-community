// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package icons;

import com.intellij.ui.IconManager;

import javax.swing.*;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

/**
 * NOTE THIS FILE IS AUTO-GENERATED
 * DO NOT EDIT IT BY HAND, run "Generate icon classes" configuration instead
 */
public final class JavaDebuggerStreamsIcons {
  private static Icon load(String path) {
    return IconManager.getInstance().getIcon(path, JavaDebuggerStreamsIcons.class);
  }

  private static Icon load(String path, Class<?> clazz) {
    return IconManager.getInstance().getIcon(path, clazz);
  }

  /** 16x16 */ public static final Icon Stream_debugger = load("/icons/stream_debugger.svg");

  /** @deprecated to be removed in IDEA 2020 - use AllIcons.Debugger.Console */
  @SuppressWarnings("unused")
  @Deprecated
  @ScheduledForRemoval(inVersion = "2020.1")
  public static final Icon Tab = load("/debugger/console.svg", com.intellij.icons.AllIcons.class);
}
