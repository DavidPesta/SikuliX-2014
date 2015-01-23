/*
 * Copyright 2010-2014, Sikuli.org, SikulixUtil.com
 * Released under the MIT License.
 *
 * modified RaiMan
 */
package org.sikuli.script;

import java.awt.Dimension;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.sikuli.basics.Debug;
import org.sikuli.basics.FileManager;
import org.sikuli.basics.HotkeyManager;
import org.sikuli.basics.PreferencesUser;
import org.sikuli.basics.ResourceLoader;
import org.sikuli.basics.Settings;

/**
 * global services for package API
 */
public class Sikulix {
  
  static RunTime runTime = null;

  private static final String me = "Sikulix: ";

  private static int lvl = 3;
	private static final String prefNonSikuli = "nonSikuli_";
  private static void log(int level, String message, Object... args) {
    Debug.logx(level, me + message, args);
  }

  private static boolean runningFromJar;
  private static String jarPath;
  private static String jarParentPath;

	static {
    if (Debug.getDebugLevel() == 0) {
      Debug.setDebugLevel(1);
    }
    runTime = RunTime.get();
    CodeSource codeSrc = Sikulix.class.getProtectionDomain().getCodeSource();
    if (codeSrc != null && codeSrc.getLocation() != null) {
      URL jarURL = codeSrc.getLocation();
      jarPath = FileManager.slashify(new File(jarURL.getPath()).getAbsolutePath(), false);
      jarParentPath = (new File(jarPath)).getParent();
      if (jarPath.endsWith(".jar")) {
        runningFromJar = true;
      } else {
        jarPath += "/";
      }
    }
	}
  
  public static int debugLevel = 1;
  private static RunTime rt = null;
	public static void main(String[] args) throws MalformedURLException {
    int dl = RunTime.checkArgs(args);
    if (dl > -1) {
      debugLevel = dl;
    }
    rt = RunTime.get();
    if (!rt.testing) {
      rt.show();
    }
    rt.testing = true;
    Debug.on(3);
    

// *********************************
// make a folder content list
    // Libswin/target/classes/META-INF/libs/windows/libs64
    if (addJarToCP("Libswin", null)) {
      rt.dumpClassPath();
    }
    File testFolder = new File(rt.fUserDir, "SikulixTest");
    FileManager.deleteFileOrFolder(testFolder.getAbsolutePath());
    rt.extractResourcesToFolder("/META-INF/libs", testFolder, new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        if (name.endsWith(".txt")) return false;
        if (name.contains("opencv")) return false;
        if (name.endsWith("/")) {
          if (name.contains("libs32")) {
            return false;
          }
        }
        return true;
      }
    });
    if (RunTime.testing && !rt.runningJar) {
      log(lvl, "testing is on - leaving");
      System.exit(1);
    }

// *********************************
// test images from jar
    File fSetupJar = null;
    if (rt.fSxProject == null) {
      fSetupJar = new File(rt.fSxBase, "sikulixsetup-1.1.0.jar");
    } else {
      fSetupJar = new File(rt.fSxProject, "Setup/target/sikulixsetup-1.1.0-plain.jar");
    }
    if (!fSetupJar.exists()) {
      log(-1, "cannot run - missing: %s", fSetupJar);
      System.exit(1);
    }
    rt.addToClasspath(fSetupJar.getPath());
    if (Debug.getDebugLevel() > 1) {
      rt.dumpClassPath();
    }
    Screen s = new Screen();
    ImagePath.add("org.sikuli.setup.RunSetup/Images");
    s.exists("SikuliLogo");
    s.exists("SikuliLogo");
    s.highlight(-2);
    log(lvl, "running main: nothing to do (yet)");
	}
  
  private static boolean addToCP(String folder) {
    if (rt.fSxProject != null) {
      rt.addToClasspath(new File(rt.fSxProject, folder + "/target/classes").getAbsolutePath());
      return true;
    }
    return false;
  }

  private static boolean addJarToCP(String folder, String filter) {
    if (rt.fSxProject != null) {
      File aFolder = new File(rt.fSxProject, folder + "/target");
      if (! aFolder.exists()) {
        return false;
      }
      for (String sFile : aFolder.list()) {
        if (sFile.endsWith(".jar")) {
          if (filter != null && !filter.isEmpty() && !sFile.contains(filter)) {
            continue;
          }
          rt.addToClasspath(new File(aFolder, sFile.toString()).getAbsolutePath());
          return true;
        }
      }
      return false;
    }
    return false;
  }

  public static boolean isRunningFromJar() {
    return runningFromJar;
  }

  public static String getJarPath() {
    return jarPath;
  }

  public static String getJarParentPath() {
    return jarParentPath;
  }

  private static boolean runningSikulixapi = false;

  /**
   * Get the value of runningSikulixUtilapi
   *
   * @return the value of runningSikulixUtilapi
   */
  public static boolean isRunningSikulixapi() {
    return runningSikulixapi;
  }

  /**
   * Set the value of runningSikulixUtilapi
   *
   * @param runningAPI new value of runningSikulixUtilapi
   */
  public static void setRunningSikulixapi(boolean runningAPI) {
    runningSikulixapi = runningAPI;
  }

  /**
   * call this, to initialize Sikuli up to useability
   * @return the primary screen object or null if headless
   */
  public static Screen init() {
		if (!canRun()) {
			return null;
		}
//TODO collect initializations here
    Mouse.init();
    Keys.init();
    return new Screen();
  }

	/**
	 * Can SikuliX be run on this machine?
	 * @return true if not running headless false otherwise
	 */
	public static boolean canRun() {
		return !RunTime.get().isHeadless();
	}

  /**
   * INTERNAL USE:
   * convenience function: runs {@link #cleanUp(int)},
   * prints a message endNormal and terminates with returncode
   * @param n
   */
  public static void endNormal(int n) {
    log(lvl, "endNormal: %d", n);
    cleanUp(n);
    System.exit(n);
  }

  /**
   * INTERNAL USE:
   * convenience function: runs {@link #cleanUp(int)},
   * prints a message endWarning and terminates with returncode
   *
   * @param n returncode
   */
  public static void endWarning(int n) {
    log(lvl, "endWarning: %d", n);
    cleanUp(n);
    System.exit(n);
  }

  /**
   * INTERNAL USE:
   * convenience function: runs {@link #cleanUp(int)},
   * prints a message endError and terminates with returncode
   *
   * @param n
   */
  public static void endError(int n) {
    log(lvl, "endError: %d", n);
    cleanUp(n);
    System.exit(n);
  }

  public static void terminate(int n) {
    String msg = "***** Terminating SikuliX Setup after a fatal error"
                        + (n == 0 ? "*****\n" : " %d *****\n")
                        +	"SikuliX is not useable!\n"
                        + "Check the error log at " + Debug.logfile;
    if (Settings.runningSetup) {
      if (Settings.noPupUps) {
        log(-1, msg, n);
      } else {
        popError(String.format(msg, n));
      }
    } else {
	    Debug.error("***** Terminating SikuliX after a fatal error"
            + (n == 0 ? "*****\n" : " %d *****\n")
            + "It makes no sense to continue!\n"
            + "If you do not have any idea about the error cause or solution, run again\n"
            + "with a Debug level of 3. You might paste the output to the Q&A board.", n);
	    cleanUp(0);
		}
    System.exit(1);
  }

  /**
   * INTERNAL USE:
   * resets stateful Sikuli X features:
   * ScreenHighlighter, Observing, Mouse, Key, Hotkeys
   * When in IDE: resets selected options to defaults (TODO)
   *
   * @param n returncode
   */
  public static void cleanUp(int n) {
    log(lvl, "cleanUp: %d", n);
    ScreenHighlighter.closeAll();
    Observing.cleanUp();
    Mouse.reset();
    //TODO move to class Keys after implementation
    Screen.getPrimaryScreen().getRobot().keyUp();
    //TODO what about remote screen sessions????
    HotkeyManager.reset();
  }

  /**
   * INTERNAL USE: used in setup: tests basic SikulixUtil features
   *
   * @return success
   */
  public static boolean testSetup() {
    return doTestSetup("Java API", false);
  }

  /**
   * INTERNAL USE: used in setup: tests basic SikulixUtil features
   *
   * @return success
   */
  public static boolean testSetup(String src) {
    return doTestSetup(src, false);
  }

  /**
   * INTERNAL USE: used in setup: tests basic SikulixUtil features
   *
   * @return success
   */
  public static boolean testSetupSilent() {
    Settings.noPupUps = true;
    return doTestSetup("Java API", true);
  }

  private static boolean doTestSetup(String testSetupSource, boolean silent) {
    Region r = Region.create(0, 0, 100, 100);
    Image img = new Image(r.getScreen().capture(r).getImage());
    Pattern p = new Pattern(img);
    Finder f = new Finder(img);
    boolean success = (null != f.find(p));
    log(lvl, "testSetup: Finder setup with image %s", (!success ? "did not work" : "worked"));
    if (success &= f.hasNext()) {
      success = (null != f.find(img.asFile()));
      log(lvl, "testSetup: Finder setup with image file %s", (!success ? "did not work" : "worked"));
      success &= f.hasNext();
      String screenFind = "Screen.find(imagefile)";
      try {
        ((Screen) r.getScreen()).find(img.asFile());
        log(lvl, "testSetup: %s worked", screenFind);
        screenFind = "repeated Screen.find(imagefile)";
        ((Screen) r.getScreen()).find(img.asFile());
        log(lvl, "testSetup: %s worked", screenFind);
      } catch (Exception ex) {
        log(lvl, "testSetup: %s did not work", screenFind);
        success = false;
      }
    }
    if (success) {
      if (!silent) {
        popup("Hallo from Sikulix.testSetup: " + testSetupSource + "\n"
                + "SikuliX seems to be working!\n\nHave fun!");
        log(lvl, "testSetup: Finder.find: worked");
      } else {
        System.out.println("[info] RunSetup: Sikulix.testSetup: Java Sikuli seems to be working!");
      }
      return true;
    }
    log(lvl, "testSetup: last Screen/Finder.find: did not work");
    return false;
  }
  
  @Deprecated
  public static boolean addToClasspath(String jar) {
    return RunTime.get().addToClasspath(jar);
  }

  @Deprecated
  public static boolean isOnClasspath(String artefact) {
    return null != RunTime.get().isOnClasspath(artefact);
  }

	public static String run(String cmdline) {
		return run(new String[]{cmdline});
	}

	public static String run(String[] cmd) {
		return ResourceLoader.get().runcmd(cmd);
	}

	public static void popError(String message) {
		popError(message, "Sikuli");
	}

	public static void popError(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * request user's input as one line of text <br>
	 * with hidden = true: <br>
	 * the dialog works as password input (input text hidden as bullets) <br>
	 * take care to destroy the return value as soon as possible (internally the password is deleted on return)
	 * @param msg
	 * @param preset
	 * @param title
	 * @param hidden
	 * @return the text entered
	 */
	public static String input(String msg, String preset, String title, boolean hidden) {
		if (!hidden) {
			if ("".equals(title)) {
				title = "Sikuli input request";
			}
			return (String) JOptionPane.showInputDialog(null, msg, title, JOptionPane.PLAIN_MESSAGE, null, null, preset);
		} else {
			preset = "";
			JTextArea tm = new JTextArea(msg);
			tm.setColumns(20);
			tm.setLineWrap(true);
			tm.setWrapStyleWord(true);
			tm.setEditable(false);
			tm.setBackground(new JLabel().getBackground());
			JPasswordField pw = new JPasswordField(preset);
			JPanel pnl = new JPanel();
			pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
			pnl.add(pw);
			pnl.add(Box.createVerticalStrut(10));
			pnl.add(tm);
			if (0 == JOptionPane.showConfirmDialog(null, pnl, title, JOptionPane.OK_CANCEL_OPTION)) {
				char[] pwc = pw.getPassword();
				String pwr = "";
				for (int i = 0; i < pwc.length; i++) {
					pwr = pwr + pwc[i];
					pwc[i] = 0;
				}
				return pwr;
			} else {
				return "";
			}
		}
	}

	public static String input(String msg, String title, boolean hidden) {
		return input(msg, "", title, hidden);
	}

	public static String input(String msg, boolean hidden) {
		return input(msg, "", "", hidden);
	}

	public static String input(String msg, String preset, String title) {
		return input(msg, preset, title, false);
	}

	public static String input(String msg, String preset) {
		return input(msg, preset, "", false);
	}

	public static String input(String msg) {
		return input(msg, "", "", false);
	}

	public static boolean popAsk(String msg) {
		return popAsk(msg, null);
	}

	public static boolean popAsk(String msg, String title) {
		if (title == null) {
			title = "... something to decide!";
		}
		int ret = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_OPTION);
		if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.NO_OPTION) {
			return false;
		}
		return true;
	}

	public static void popup(String message) {
		popup(message, "Sikuli");
	}

	public static void popup(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
	}

	public static String popSelect(String msg, String[] options, String preset) {
		return popSelect(msg, null, options, preset);
	}

	public static String popSelect(String msg, String[] options) {
    if (options.length == 0) {
      return "";
    }
		return popSelect(msg, null, options, options[0]);
	}

	public static String popSelect(String msg, String title, String[] options) {
    if (options.length == 0) {
      return "";
    }
		return popSelect(msg, title, options, options[0]);
	}

	public static String popSelect(String msg, String title, String[] options, String preset) {
		if (title == null || "".equals(title)) {
			title = "... something to select!";
		}
    if (options.length == 0) {
      return "";
    }
    if (preset == null) {
      preset = options[0];
    }
		return (String) JOptionPane.showInputDialog(null, msg, title, JOptionPane.PLAIN_MESSAGE, null, options, preset);
	}

	/**
	 * Shows a dialog request to enter text in a multiline text field <br>
	 * Though not all text might be visible, everything entered is delivered with the returned text <br>
	 * The main purpose for this feature is to allow pasting text from somewhere preserving line breaks <br>
	 * @param msg the message to display.
	 * @param title the title for the dialog (default: Sikuli input request)
	 * @param lines the maximum number of lines visible in the text field (default 9)
	 * @param width the maximum number of characters visible in one line (default 20)
	 * @return The user's input including the line breaks.
	 */
	public static String inputText(String msg, String title, int lines, int width) {
		width = Math.max(20, width);
		lines = Math.max(9, lines);
		if ("".equals(title)) {
			title = "Sikuli input request";
		}
		JTextArea ta = new JTextArea("");
		int w = width * ta.getFontMetrics(ta.getFont()).charWidth('m');
		int h = (int) (lines * ta.getFontMetrics(ta.getFont()).getHeight());
		ta.setPreferredSize(new Dimension(w, h));
		ta.setMaximumSize(new Dimension(w, 2 * h));
		JScrollPane sp = new JScrollPane(ta);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		JTextArea tm = new JTextArea(msg);
		tm.setColumns(width);
		tm.setLineWrap(true);
		tm.setWrapStyleWord(true);
		tm.setEditable(false);
		tm.setBackground(new JLabel().getBackground());
		JPanel pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
		pnl.add(sp);
		pnl.add(Box.createVerticalStrut(10));
		pnl.add(tm);
		pnl.add(Box.createVerticalStrut(10));
		if (0 == JOptionPane.showConfirmDialog(null, pnl, title, JOptionPane.OK_CANCEL_OPTION)) {
			return ta.getText();
		} else {
			return "";
		}
	}

	public static void pause(int time) {
		try {
			Thread.sleep(time * 1000);
		} catch (InterruptedException ex) {
		}
	}

	public static void pause(float time) {
		try {
			Thread.sleep((int) (time * 1000));
		} catch (InterruptedException ex) {
		}
	}

	public static boolean importPrefs(String path) {
		return true;
	}

	public static String arrayToString(String[] args) {
		String ret = "";
		for (String s : args) {
			if (s.contains(" ")) {
				s = "\"" + s + "\"";
			}
			ret += s + " ";
		}
		return ret;
	}

	public static boolean exportPrefs(String path) {
		return true;
	}

	/**
	 * store a key-value-pair in Javas persistent preferences storage that is used
	 * by SikuliX to save settings and information between IDE sessions<br>
	 * this allows, to easily make some valuable information persistent
	 * @param key name of the item
	 * @param value item content
	 */
	public static void prefStore(String key, String value) {
		PreferencesUser.getInstance().put(prefNonSikuli + key, value);
	}

	/**
	 * retrieve the value of a previously stored a key-value-pair
	 * from Javas persistent preferences storage that is used
	 * by SikuliX to save settings and information between IDE sessions<br>
	 * @param key name of the item
	 * @return the item content or empty string if not stored yet
	 */
	public static String prefLoad(String key) {
		return PreferencesUser.getInstance().get(prefNonSikuli + key, "");
	}

	/**
	 * retrieve the value of a previously stored a key-value-pair
	 * from Javas persistent preferences storage that is used
	 * by SikuliX to save settings and information between IDE sessions<br>
	 * @param key name of the item
	 * @param value the item content or the given value if not stored yet (default)
	 * @return the item content or the given default
	 */
	public static String prefLoad(String key, String value) {
		return PreferencesUser.getInstance().get(prefNonSikuli + key, value);
	}

	/**
	 * permanently remove the previously stored key-value-pair having the given key
	 * from Javas persistent preferences storage that is used
	 * by SikuliX to save settings and information between IDE sessions<br>
	 * @param key name of the item to permanently remove
	 * @return the item content that would be returned by prefLoad(key)
	 */
	public static String prefRemove(String key) {
		String val = prefLoad(key);
		PreferencesUser.getInstance().remove(prefNonSikuli + key);
		return val;
	}

	/**
	 * permanently remove all previously stored key-value-pairs (by prefsStore())
	 * from Javas persistent preferences storage that is used
	 * by SikuliX to save settings and information between IDE sessions<br>
	 */
	public static void prefRemove() {
		PreferencesUser.getInstance().removeAll(prefNonSikuli);
	}
}
