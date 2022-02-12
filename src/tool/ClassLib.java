package tool;

import annotation.Controller;
import annotation.UrlMapping;
import annotation.UrlMappingModel;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class ClassLib {

    public static Method[] matchMethods(Class objClass, String[] attribs, String funcType) {
        List<Method> methodList = new ArrayList<Method>();
        Method[] classMethods = objClass.getMethods();
        for (int x = 0; x < attribs.length; x++)
            for (int i = 0; i < classMethods.length; i++)
                if (classMethods[i].getName().startsWith(funcType))
                    if (attribs[x].equalsIgnoreCase(classMethods[i].getName().split(funcType)[1]))
                        methodList.add(classMethods[i]);
        Method[] ret = new Method[methodList.size()];
        int index = 0;
        for (Method method : methodList) {
            ret[index] = method;
            index++;
        }
        return ret;
    }

    public static String[] getKeys(HashMap<String, String> attribVal){
        Object[] strings = attribVal.keySet().toArray();
        String[] res = new String[strings.length];
        for(int i=0;i<res.length;i++){
            res[i] = (String) attribVal.keySet().toArray()[i];
            System.out.println(res[i]);
        }
        return res;
    }
    public static Object createInstance(Class objClass, HashMap<String, String> attribVal, String[] values) throws Exception{
        Object instance = null;
            instance = objClass.newInstance();
            String[] keys = getKeys(attribVal);
            Method[] lMethods = matchMethods(objClass, keys,"set");
            for(int i=0;i<keys.length;i++){
                if(attribVal.get(keys[i]).equals("int") || attribVal.get(keys[i]).equals("Integer")){
                    lMethods[i].invoke(instance, Integer.parseInt(values[i]));
                }
                else if (attribVal.get(keys[i]).equals("float") || attribVal.get(keys[i]).equals("Float")){
                    lMethods[i].invoke(instance, Float.parseFloat(values[i]));
                }
                else if (attribVal.get(keys[i]).equals("String")){
                    lMethods[i].invoke(instance, values[i]);
                }
                else if (attribVal.get(keys[i]).equals("Date") || attribVal.get(keys[i]).equals("Calendar")){
                    if(!values[i].equals("")){
                        int year = 0;
                        int month = 0;
                        int date = 0;
                        String[] attrDate = new String[1];
                        if(values[i].contains("-")){
                            attrDate = values[i].split("-");
                        }
                        else if(values[i].contains("/")) {
                            attrDate = values[i].split("/");
                        }
                        if (attrDate[0].length()==4){
                            year = Integer.parseInt(attrDate[0]);
                            month = Integer.parseInt(attrDate[1])-1;
                            date = Integer.parseInt(attrDate[2]);
                        }
                        else{
                            year = Integer.parseInt(attrDate[2]);
                            month = Integer.parseInt(attrDate[1])-1;
                            date = Integer.parseInt(attrDate[0]);
                        }
                        if(attribVal.get(keys[i]).equals("Date")){
                            lMethods[i].invoke(instance, new Date(year-1900, month, date));
                        }
                        else{
                            Calendar cal = Calendar.getInstance();
                            cal.set(year,month,date);
                            lMethods[i].invoke(instance, cal);
                        }
                    }
                }
            }
        return instance;
    }

    public static String match(String[] attrib, String obj, String prefix){
        for(int i=0;i<attrib.length;i++){
            if(obj.equalsIgnoreCase(attrib[i].split(prefix)[1])) return	obj;
        }
        return "";
    }

    public static String capitalize(String str)
    {
        if(str == null) return null;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static  Method getSetter(Class className, String attribName, Class objType){
        Method setter = null;
        System.out.println(className.getName());
        try{
            setter = className.getMethod("set"+capitalize(attribName),objType);
        }catch (NoSuchMethodException e){
            e.printStackTrace();
        }
        return setter;
    }

    public static Class[] getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assert loader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = loader.getResources(path);
        List<File> directories = new ArrayList<File>();
        while(resources.hasMoreElements()){
            URL url = resources.nextElement();
            directories.add(new File(url.getFile()));
        }
        ArrayList<Class> classList = new ArrayList<Class>();
        for(File dir: directories){
            classList.addAll(findClasses(dir, packageName));
        }
        return classList.toArray(new Class[classList.size()]);
    }

    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        List<Class> validClass = new ArrayList<Class>();
        for(Class classe: classes){
            if(isController(classe)) validClass.add(classe);
        }
        return validClass;
    }

    /*
     * pour acceder au fichier de configuration
     * */
    public static String getWorkingDirectory(ServletContext context){
        String workingDir = "";
        File file = new File(context.getRealPath("/WEB-INF/config/appConfig.properties"));
        try{
            Scanner fileScanner = new Scanner(file);
            while(fileScanner.hasNextLine()){
                String data = fileScanner.nextLine();
                if(!data.contains("#")){
                    if(data.startsWith("workingDirectory")){
                        workingDir = data.split("workingDirectory=")[1];
                        break;
                    }
                }
            }
        }catch (Exception e){
            System.out.println("Un erreur est survenu pendant la configuration du document de travail");
            e.printStackTrace();
        }
        return  workingDir;
    }
    public static String getUploadDirectory(ServletContext context){
        String uploadDir= "";
        File file = new File(context.getRealPath("/WEB-INF/config/appConfig.properties"));
        Scanner fileScanner = null;
        try{
            fileScanner = new Scanner(file);
            while(fileScanner.hasNextLine()){
                String data = fileScanner.nextLine();
                if(!data.contains("#")){
                    if(data.startsWith("uploadDirectory")){
                        uploadDir = data.split("uploadDirectory=")[1];
                        break;
                    }
                }
            }
        }catch (Exception e){
            System.out.println("appConfig.properties could not be loaded");
            e.printStackTrace();
        } finally {
            assert fileScanner != null;
            fileScanner.close();
        }
        return uploadDir;
    }


    /*
    * retrieve all UrlMapping annotations from all the classes' methods
    *
    * */
    public static UrlMappingModel[] getAnnotations(String workingDirectory)throws Exception{
        Class[] controllers = getClasses(workingDirectory);
        List<UrlMappingModel> listUrl = new ArrayList<>();
        for(Class controller: controllers){
            Method[] methods = controller.getMethods();
            for(Method method: methods){
                Annotation[] annotations = method.getDeclaredAnnotations();
                if(annotations.length != 0){
                    for(Annotation annotation: annotations){
                        if(annotation instanceof UrlMapping){
                            UrlMapping url = (UrlMapping) annotation;
                            listUrl.add(new UrlMappingModel(method.getName(), controller.getName(), url.view(), url.url()));
                        }
                    }
                }
            }
        }
        return listUrl.toArray(new UrlMappingModel[listUrl.size()]);
    }

    /*
    * to check if a class is a controller
    * */
    public static boolean isController(Class classe){
        Annotation[] annotations = classe.getDeclaredAnnotations();
        for(Annotation annotation: annotations){
            if(annotation instanceof Controller) return true;
        }
        return false;
    }

    public static UrlMappingModel findMatchingUrl(String url, UrlMappingModel[] dataUrls) throws IOException, ClassNotFoundException {
        if(dataUrls == null) throw new UnsupportedOperationException();
        for(UrlMappingModel urlMappingModel: dataUrls){
            if(urlMappingModel.getUrl().equals(url)) return urlMappingModel;
        }
        return null;
    }
}
