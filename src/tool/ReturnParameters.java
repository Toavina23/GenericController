package tool;

import java.util.HashMap;

public class ReturnParameters {
    HashMap<String, Object> returnValues;
    String viewUrl;

    public ReturnParameters(HashMap<String, Object> returnValues, String viewUrl) {
        this.returnValues = returnValues;
        this.viewUrl = viewUrl;
    }

    public HashMap<String, Object> getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(HashMap<String, Object> returnValues) {
        this.returnValues = returnValues;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }
}
