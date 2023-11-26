package org.el.ghostnetfishing.view;

import java.io.IOException;
import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Named;

import org.el.ghostnetfishing.model.Person;

/**
 * UserManagementBean is used for user management.
 * @author Eugen
 */

@Named
@SessionScoped
public class UserManagementBean implements Serializable {

	/**
	 * Uniquely identify the version of a serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * {@link org.el.ghostnetfishing.model.Person user} for log in.
	 */
	private Person user;

	public Person getUser() {
		return user;
	}

	public void setUser(Person user) {
		this.user = user;
	}

	/**
	 * Used for user login.
	 * @param trigger an Ajax event when an component changed and updates this element.
	 * @throws IOException if index.xhtml page doesn't exists, throws an error.
	 */
	public void onPersonSelection(AjaxBehaviorEvent event) throws IOException {
		// When triggered, redirects to the index.xhtml
		FacesContext.getCurrentInstance().getExternalContext().redirect("/ghostNetFishing/faces/index.xhtml");
		FacesContext.getCurrentInstance().responseComplete();
	}

	/**
	 * Used for user logout.
	 * @throws IOException by an error.
	 */
	public void resetUser() throws IOException {
		// Set the current user to null
		this.user = null;
		
		// When triggered, redirects to the index.xhtml
		FacesContext.getCurrentInstance().getExternalContext().redirect("/ghostNetFishing/faces/index.xhtml");
		FacesContext.getCurrentInstance().responseComplete();
	}
}
