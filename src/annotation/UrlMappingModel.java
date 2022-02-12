package annotation;

public class UrlMappingModel {

    String methodName;
    String controllerName;
    String view;
    String url;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UrlMappingModel(String methodName, String controllerName, String view, String url) {
        this.methodName = methodName;
        this.controllerName = controllerName;
        this.view = view;
        this.url = url;
    }
}