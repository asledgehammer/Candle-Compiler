
import zombie.Lua.LuaManager;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Test {

  public static BufferedReader getFileReader(String var0, boolean var1) throws IOException {
    if (containsDoubleDot(var0)) {
      System.err.println("relative paths not allowed");
      return null;
    } else {
      String var2 = getLuaCacheDir() + File.separator + var0;
      var2 = var2.replace("/", File.separator);
      var2 = var2.replace("\\", File.separator);
      File var3 = new File(var2);
      if (!var3.exists() && var1) {
        var3.createNewFile();
      }

      if (var3.exists()) {
        BufferedReader var4 = null;

        try {
          FileInputStream var5 = new FileInputStream(var3);
          InputStreamReader var6 = new InputStreamReader(var5, StandardCharsets.UTF_8);
          var4 = new BufferedReader(var6);
        } catch (IOException var7) {
          Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, var7);
        }

        return var4;
      } else {
        return null;
      }
    }
  }

  public static LuaManager.GlobalObject.LuaFileWriter getFileWriter(String var0, boolean var1, boolean var2) {
    if (containsDoubleDot(var0)) {
      System.err.println("relative paths not allowed");
      return null;
    } else {
      String var3 = LuaManager.getLuaCacheDir() + File.separator + var0;
      var3 = var3.replace("/", File.separator);
      var3 = var3.replace("\\", File.separator);
      String var4 = var3.substring(0, var3.lastIndexOf(File.separator));
      var4 = var4.replace("\\", "/");
      File var5 = new File(var4);
      if (!var5.exists()) {
        var5.mkdirs();
      }

      File var6 = new File(var3);
      if (!var6.exists() && var1) {
        try {
          var6.createNewFile();
        } catch (IOException var11) {
          Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, var11);
        }
      }

      PrintWriter var7 = null;

      try {
        FileOutputStream var8 = new FileOutputStream(var6, var2);
        OutputStreamWriter var9 = new OutputStreamWriter(var8, StandardCharsets.UTF_8);
        var7 = new PrintWriter(var9);
      } catch (IOException var10) {
        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, var10);
      }

      return new LuaManager.GlobalObject.LuaFileWriter(var7);
    }
  }

  public static String getLuaCacheDir() {
    String path = getCacheDir() + File.separator + "Lua";
    File file = new File(path);
    if (!file.exists()) file.mkdir();
    return path;
  }

  public static String getCacheDir() {
    return System.getProperty("user.home") + File.separator + "Zomboid";
  }

  public static boolean containsDoubleDot(String var0) {
    if (isNullOrEmpty(var0)) {
      return false;
    } else {
      return var0.contains("..") || var0.contains("\u0000.\u0000.");
    }
  }

  public static boolean isNullOrEmpty(String var0) {
    return var0 == null || var0.length() == 0;
  }

  public static void main(String[] yargs) throws IOException {
    LuaManager.GlobalObject.LuaFileWriter writer = getFileWriter("Test/hello_world.txt", false, false);
    System.out.println(writer);

    writer.write("This is from Java! (Junction)");

    if(writer != null) writer.close();


    Field f = Test.class.getFields()[0];



  }
}
