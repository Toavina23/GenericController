package servlet;

import annotation.UrlMappingModel;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import tool.ClassLib;
import tool.ReturnParameters;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GenericServlet extends HttpServlet {

    private UrlMappingModel[] urlMappingModels = null;

    private static  String relPath = null;
    private int counter = 0;
    private  String workingDirectory = "";
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception{
        workingDirectory = ClassLib.getWorkingDirectory(request.getServletContext());
        boolean formRequest = false;
        String url = new String(request.getRequestURL()).substring(request.getContextPath().length());
        if(url.contains("/form/")) formRequest = true;
        String cleanUrl = getCleanUrl(url);
        String[] splittedUrl = cleanUrl.split("-");
        String view = "";
        if(formRequest){
            /* Verifier par methode d'annotation*/
            Class controller = null;
            Method targetMethod = null;
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            List<FileItem> parts = null;
            String[] parameters = null;
            try{
                controller = Class.forName(workingDirectory+"."+splittedUrl[0]+"Controller");
            }catch (ClassNotFoundException ex){
                UrlMappingModel model = handleAnnotationFormRequest(url);
                controller = Class.forName(model.getControllerName());
                targetMethod = controller.getMethod(model.getMethodName());
                view = "/"+model.getView();
            }
            if(isMultipart) {
                if(relPath == null) relPath = ClassLib.getUploadDirectory(request.getServletContext());
                parts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
                parameters = getMultipartObjectParameters(request, parts);
            }
            else{
                parameters = getObjectParameters(request);
            }
            String controllerAttrib = null;
            if(!(controllerAttrib = matchClass(controller, parameters)).equals("")){
                String[] values = null;
                String attribName = controllerAttrib.split(" ")[1];
                String className = controllerAttrib.split(" ")[0];
                HashMap<String, String> validParams = finalAttributes(parameters, attribName, className);
                if(isMultipart){
                    values = multipartValuesOf(validParams, request, attribName, parts);
                }
                else{
                    values = formValueOf(validParams, request, attribName);
                }
                Class objClass = Class.forName(className);
                Object attribInstance = ClassLib.createInstance(objClass, validParams, values);
                Object controllerInstance = controller.newInstance();
                Method attribSetter = ClassLib.getSetter(controller, attribName, objClass);
                attribSetter.invoke(controllerInstance, attribInstance);

                if(targetMethod == null){
                    try{
                        targetMethod = controller.getMethod(splittedUrl[1]);
                    }catch (NoSuchMethodException ex){
                        throw ex;
                    }
                }
                view = setRequestParameters(request, targetMethod, controllerInstance);
            }
        }else{
            Class controller = null;
            boolean annotation = false;
            try {
                controller = Class.forName(workingDirectory+".controller."+splittedUrl[0]+"Controller");
            }catch (ClassNotFoundException ex){
                view = handleAnnotationRequest(url, request, response);
                annotation = true;
            }
            if(!annotation){
                Method targetMethod = controller.getMethod(splittedUrl[1], null);
                view = setRequestParameters(request, targetMethod, controller.newInstance());
            }
        }
        String pathView = "";
        if(splittedUrl.length > 1){
            pathView = "/" + splittedUrl[0]+"/"+splittedUrl[1]+".jsp";
        }else{
            pathView = "/"+splittedUrl[0];
        }
        if(!view.equals("")) pathView = view;
        request.getRequestDispatcher(pathView).forward(request,response);
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try{
            processRequest(request,response);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try{
            processRequest(request,response);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /*
     * Pour la gestion des formulaires avec upload de fichier
     * */
    public static String[] multipartValuesOf(HashMap<String, String> finalAttribs, HttpServletRequest request, String prefix, List<FileItem> parts) throws Exception{
        Object[] params = finalAttribs.keySet().toArray();
        String[] values = new String[params.length];
        for(int i=0;i<params.length;i++){
            values[i] = getPartValue(prefix+"."+(String) params[i], request, parts);
        }
        return values;
    }

    public static String getPartValue(String fieldName, HttpServletRequest request,List<FileItem> parts ) throws Exception{
        String returnValue = "";
        for(FileItem item: parts){
            if(item.getFieldName().equals(fieldName)){
                if(!item.isFormField()){
                    LocalDateTime myDateObj = LocalDateTime.now();
                    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    String now = myDateObj.format(myFormatObj).replace(" ", "-").replace(":","-")+"-";
                    String fileName = new File(now+item.getName()).getName();
                    item.write(new File(relPath.replace("/", "\\")+File.separator+fileName));
                    returnValue = relPath+"/"+fileName;
                }
                else{
                    String fieldValue = new String(item.get());
                    returnValue =  fieldValue;
                }
            }
        }
        return returnValue;
    }



    /*
     * Pour gerer les formulaires simples
     * */
    public static String[] formValueOf(HashMap<String, String> finalAttribs, HttpServletRequest request, String prefix){
        Object[] params = finalAttribs.keySet().toArray();
        String[] values = new String[params.length];
        for(int i=0;i<params.length;i++){
            values[i] = request.getParameter(prefix+"."+(String) params[i]);
        }
        return values;
    }

    /*
     * Pour recuperer les parametres avec un "point" dans leur nom
     * */

    public static String[] getMultipartObjectParameters(HttpServletRequest request,List<FileItem> parts) throws Exception{
        List<String> parameterNames = new ArrayList<>();
        int i = 0;
        for(FileItem item: parts){
            String temp = item.getFieldName();
            if(temp.contains("."))
                parameterNames.add(temp);
        }
        return parameterNames.toArray(new String[parameterNames.size()]);
    }
    public static String[] getObjectParameters(HttpServletRequest request){
        Vector<String> tempStrings = new Vector<String>(1,1);
        Enumeration<String> enumParam = request.getParameterNames();
        String temp = "";
        while(enumParam.hasMoreElements()){
            temp = enumParam.nextElement();
            if(temp.contains(".")){
                tempStrings.add(temp);
            }
        }
        String[] res = new String[tempStrings.size()];
        for(int i=0;i<tempStrings.size();i++){
            res[i] = tempStrings.elementAt(i);
        }
        return res;
    }

    /*
     * Pour verifier si le controlleur demandé posséde l'attribut des parametres
     * */
    public static String matchClass(Class controllerClass, String[] validParams){
        String model = validParams[0].split("\\.")[0];
        try{
            System.out.println(controllerClass.getName());
            Field attrib = controllerClass.getDeclaredField(model);
            return attrib.getType().getName()+" "+attrib.getName();
        }catch (NoSuchFieldException e){
            e.printStackTrace();
            return "";
        }
    }


    /*
     * pour matcher les attributs en parametre avec ceux du modele
     * */
    public static HashMap<String, String> finalAttributes(String[] validAttribs, String attribName, String className){
        Class modelClass = null;
        try{
            modelClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.out.println("Classe du modèle non trouvée");
            e.printStackTrace();
        }
        Field[] modelFiels = modelClass.getDeclaredFields();
        HashMap<String, String> res = new HashMap<String,String>();
        for(int i=0; i< modelFiels.length;i++){
            if(!ClassLib.match(validAttribs, modelFiels[i].getName(), attribName+".").equals("")){
                res.put(modelFiels[i].getName(), modelFiels[i].getType().getSimpleName());
            }
        }
        return res;
    }
    /*
     * pour recuperer le nom du contolleur
     * */
    public static String getControllerName(String url){
        url = url.split("-")[0];
        System.out.println(ClassLib.capitalize(url)+"Controller");
        return ClassLib.capitalize(url)+"Controller";
    }

    /*
     * pour recuperer l'url sans les verbes de convetions
     * */
    public static String getCleanUrl(String rawUrl){
        String className = "";
        System.out.println(rawUrl);
        if(rawUrl.contains("/request/")) {
            className = rawUrl.split("/request/")[1];
            if (className.contains("form/"))
                className = className.split("form/")[1];
        }
        int indexSlash=className.lastIndexOf("/");
        className=className.substring(indexSlash+1);
        return className;
    }

    /*
     * pour recuperer les valeurs de retour de les valeurs de retour
     * de la fonction et les mettres dans un hashmap
     * */

    public static String setRequestParameters(HttpServletRequest request, Method method, Object obj) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        ReturnParameters retData = (ReturnParameters) method.invoke(obj);
        if(retData == null) System.out.println("Return data null");
        HashMap<String, Object> retValues = retData.getReturnValues();
        Object[] objKeys = retValues.keySet().toArray();
        for (Object objKey : objKeys) {
            System.out.println("Key=" + (String) objKey + "Value=" + retValues.get((String) objKey));
            String tempKey = (String) objKey;
            if (tempKey.startsWith("$Session_")) {
                request.getSession().setAttribute(tempKey.split("\\$Session_")[1], retValues.get(tempKey));
            } else {
                request.setAttribute(tempKey, retValues.get(tempKey));
            }

        }
        return "/"+retData.getViewUrl();
    }
    /*
     * Request by annotation handler
     * */
    public String handleAnnotationRequest(String url, HttpServletRequest req, HttpServletResponse res) throws Exception{
        String filteredUrl = url.split("/request/")[1];
        UrlMappingModel model = null;
        if(this.urlMappingModels == null) {
            urlMappingModels = ClassLib.getAnnotations(this.workingDirectory);
            counter++;
            System.out.println("Searched the annotation class for "+counter+" times");
        }
        if((model=ClassLib.findMatchingUrl(filteredUrl,this.urlMappingModels))!=null){
            Class controllerClass = Class.forName(model.getControllerName());
            Method method = controllerClass.getMethod(model.getMethodName());
            setRequestParameters(req, method, controllerClass.newInstance());
            return "/"+model.getView();
        }
        else throw new UnsupportedOperationException();
    }

    public UrlMappingModel handleAnnotationFormRequest(String url) throws Exception {
        String filteredUrl = url.split("/request/form/")[1];
        System.out.println(filteredUrl);
        UrlMappingModel model = null;
        if(this.urlMappingModels == null) {
            urlMappingModels = ClassLib.getAnnotations(this.workingDirectory);
            counter++;
            System.out.println("Searched the annotation class for "+counter+" times");
            System.out.println("nombre d'annotations="+urlMappingModels.length);
        }
        model=ClassLib.findMatchingUrl(filteredUrl,this.urlMappingModels);
        if(model==null) throw new UnsupportedOperationException();
        return model;
    }
}
