package org.el.ghostnetfishing.view;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateful;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.el.ghostnetfishing.model.Fishnet;
import org.el.ghostnetfishing.model.FishnetState;
import org.el.ghostnetfishing.model.PersonType;

/**
 * FishnetOverviewBean is used for overview and fishnet status administration
 * @author Eugen
 */

@Named
@Stateful
@ViewScoped
public class FishnetOverviewBean {

	/**
	 * Page counter for UI.
	 */
	private int page;
	
	/**
	 * {@link org.el.ghostnetfishing.model.Fishnet fishnets} collection for fishnets. 
	 */
	private List<Fishnet> pageItems;
	
	/**
	 * Used as search parameter in search view.
	 */
	private Fishnet example = new Fishnet();

	/**
	 * Loading entity manager with unit name ghostNetFishing-persistence-unit.
	 */
	@PersistenceContext(unitName = "ghostNetFishing-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return 10;
	}

	public Fishnet getExample() {
		return this.example;
	}

	public void setExample(Fishnet example) {
		this.example = example;
	}

	public List<Fishnet> getPageItems() {
		return pageItems;
	}

	public void setPageItems(List<Fishnet> pageItems) {
		this.pageItems = pageItems;
	}

	/**
	 * Fetching all relevant fishnet states
	 * @param fishnetState of fishnet
	 * @param type of the person
	 * @return list of FishnetState, if available, if not, returns null.
	 */
	public List<FishnetState> fetchAllPossibleDestinationStates(FishnetState fishnetState, PersonType type){
		
		if(type!= null){
			try {
				return FishnetStateBean.fetchAllPossibleDestinationStates(this.entityManager, fishnetState, type);
			} catch (IllegalArgumentException e) {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				FacesMessage facesMessage = new FacesMessage("Benutzer nicht ausgewählt!");
				facesContext.addMessage(null, facesMessage);
			}
		}
		return null;
	}
	
	/**
	 * Start a new search on the page 0.
	 * @return null.
	 */
	public String search() {
		this.page = 0;
		return null;
	}

	/**
	 * Allows access to the methods of the userManagementBean.
	 */
	@Inject 
	private UserManagementBean userManagementBean;
	
	/**
	 * Accepting changes on the page.
	 * If FishnetState is changed, updates the database with new dataset.
	 */
	public void accept(){
		
		for(Fishnet fishnet : this.pageItems){
			if(fishnet.getNewFishnetState() != null){
				
				// Set user for recover
				fishnet.setPersonByRecoveryMessagingPersonId(userManagementBean.getUser());
				
				// Updating new status for fishnet
				fishnet.setFishnetState(fishnet.getNewFishnetState());
				
				// Updating the database
				this.entityManager.merge(fishnet);
			}
		}
	}

	/**
	 * When the user paginates through the page, it retrieves data for display a list of items on the page.
	 */
	public void paginate() {

		// Restricts query results based on specific conditions 
		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		
		// Creates SQL statement with condition of criteria query
		CriteriaQuery<Fishnet> criteria = builder.createQuery(Fishnet.class);
		
		// references entities of the fishnet 
		Root<Fishnet> root = criteria.from(Fishnet.class);

		// building SQL statement
		TypedQuery<Fishnet> query = this.entityManager.createQuery(
				
				// SQL statement from search predicates as condition
				criteria.select(root).where(getSearchPredicates(root)));
		
		// Runs SQL select statement and allocates objects from getResultList to the pageItems
		this.pageItems = query.getResultList();
	}

	/**
	 * Returns an array with search predicates as condition for paginate method.
	 * @param root as Fishnet object.
	 * @return array of search predicates.
	 */
	private Predicate[] getSearchPredicates(Root<Fishnet> root) {

		// Restricts query results based on specific conditions 
		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();
		// initialize a list with all fishnet states which are relevant for recover
		List<FishnetState> states = FishnetStateBean.getAllRelevantForRecovery(this.entityManager);

		// Creates a filter for fishnetState
		In<FishnetState> inClause = builder.in(root.get("fishnetState"));
		for (FishnetState state : states) {
			inClause.value(state);
		}
		
		predicatesList.add(inClause);
		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}
}
