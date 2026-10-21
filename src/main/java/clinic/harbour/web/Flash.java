package clinic.harbour.web;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
public final class Flash {private Flash(){} public static void success(String text){FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_INFO,text,null));}public static void error(Exception e){String text=e.getMessage()==null?"The action could not be completed":e.getMessage();FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,text,null));}}
