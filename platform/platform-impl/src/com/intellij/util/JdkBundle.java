// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.util.Bitness;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.Version;
import com.intellij.util.lang.JavaVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JdkVersionDetector;
import org.jetbrains.jps.model.java.JdkVersionDetector.JdkVersionInfo;

import java.io.File;

public class JdkBundle {
  private static final String BUNDLED_JDK_DIR_NAME = "jbr";

  private final File myLocation;
  private final JdkVersionInfo myVersionInfo;
  private final boolean myBoot;
  private final boolean myBundled;
  private final boolean myJdk;

  private JdkBundle(File location, JdkVersionInfo versionInfo, boolean boot, boolean bundled, boolean jdk) {
    myLocation = location;
    myVersionInfo = versionInfo;
    myBoot = boot;
    myBundled = bundled;
    myJdk = jdk;
  }

  @NotNull
  public File getLocation() {
    return myLocation;
  }

  @NotNull
  public JavaVersion getBundleVersion() {
    return myVersionInfo.version;
  }

  @NotNull
  public Bitness getBitness() {
    return myVersionInfo.bitness;
  }

  public boolean isBoot() {
    return myBoot;
  }

  public boolean isBundled() {
    return myBundled;
  }

  public boolean isJdk() {
    return myJdk;
  }

  @NotNull
  public File getHome() {
    return getVMExecutable().getParentFile().getParentFile();
  }

  @NotNull
  public File getVMExecutable() {
    File home = myLocation;
    if (SystemInfo.isMac) {
      File contents = new File(home, "Contents/Home");
      if (contents.isDirectory()) {
        home = contents;
      }
    }
    File javaPath = new File(home, SystemInfo.isWindows ? "bin\\java.exe" : "bin/java");
    if (!javaPath.isFile()) {
      javaPath = new File(home, SystemInfo.isWindows ? "jre\\bin\\java.exe" : "jre/bin/java");
    }
    return javaPath;
  }

  public boolean isOperational() {
    if (myBoot) return true;

    File javaPath = getVMExecutable();

    try {
      ProcessOutput output = ExecUtil.execAndGetOutput(new GeneralCommandLine(javaPath.getPath(), "-version"));
      return output.getExitCode() == 0;
    }
    catch (ExecutionException e) {
      Logger.getInstance(JdkBundle.class).error(e);
      return false;
    }
  }


  @NotNull
  public static JdkBundle createBoot() {
    File home = new File(SystemProperties.getJavaHome());
    JdkBundle bundle = createBundle(home, true);
    assert bundle != null : home;
    return bundle;
  }

  @Nullable
  public static JdkBundle createBundled() {
    return createBundle(new File(PathManager.getHomePath(), BUNDLED_JDK_DIR_NAME), false);
  }

  @Nullable
  public static JdkBundle createBundle(@NotNull File bundleHome) {
    return createBundle(bundleHome, false);
  }

  private static JdkBundle createBundle(File bundleHome, boolean boot) {
    if ("jre".equals(bundleHome.getName())) {
      File jdk = bundleHome.getParentFile();
      if (new File(jdk, "lib").isDirectory()) {
        bundleHome = jdk;
      }
    }

    File actualHome = bundleHome;
    if (SystemInfo.isMac) {
      if (actualHome.getName().equals("Home") && actualHome.getParentFile().getName().equals("Contents")) {
        bundleHome = actualHome.getParentFile().getParentFile();
      }
      else {
        File contents = new File(bundleHome, "Contents/Home");
        if (contents.isDirectory()) {
          actualHome = contents;
        }
      }
    }

    JdkVersionInfo versionInfo;
    if (boot) {
      versionInfo = new JdkVersionInfo(JavaVersion.current(), SystemInfo.is64Bit ? Bitness.x64 : Bitness.x32);
    }
    else {
      versionInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(actualHome.getPath());
    }
    if (versionInfo != null) {
      boolean bundled = PathManager.isUnderHomeDirectory(bundleHome.getPath());
      boolean jdk = JdkUtil.checkForJdk(actualHome);
      return new JdkBundle(bundleHome, versionInfo, boot, bundled, jdk);
    }

    return null;
  }

  //<editor-fold desc="Deprecated stuff.">
  /** @deprecated use {@link #getBundleVersion()} */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public Version getVersion() {
    JavaVersion v = myVersionInfo.version;
    return v.feature <= 8 ? new Version(1, v.feature, 0) : new Version(v.feature, 0, v.update);
  }

  /** @deprecated use {@link #createBundle(File)} */
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  @Deprecated
  public static JdkBundle createBundle(@NotNull File jvm, boolean boot, @SuppressWarnings("unused") boolean bundled) {
    JdkBundle bundle = createBundle(jvm, boot);
    if (bundle != null) {
      Bitness arch = SystemInfo.is64Bit ? Bitness.x64 : Bitness.x32;
      if (arch != bundle.myVersionInfo.bitness) {
        bundle = null;
      }
    }
    return bundle;
  }
  //</editor-fold>
}