package org.el.ghostnetfishing.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.el.ghostnetfishing.model.Fishnet;
import org.el.ghostnetfishing.model.FishnetState;
import org.el.ghostnetfishing.model.PersonType;

import java.util.Iterator;

/**
 * Backing bean for FishnetState entities.
 * <p/>
 * This class provides CRUD functionality for all FishnetState entities. It
 * focuses purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt>
 * for state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class FishnetStateBean implements Serializable {

	/**
	 * Uniquely identify the version of a serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constant value for anonymous person
	 */
	private static final int PERSON_TYPE_ANONYMOUS = 1;
	
	/**
	 * Constant value for recoverer person 
	 */
	private static final int PERSON_TYPE_RECOVERER = 3;
	
	/**
	 * Constant value for fishnet state reported
	 */
	private static final int STATE_REPORTED = 1;
	
	/**
	 * Constant value for fishnet state recovery pending
	 */
	private static final int STATE_RECOVERY_PENDING = 2;
	
	/**
	 * Constant value for fishnet state recovered
	 */
	private static final int STATE_RECOVERED = 3;
	
	/**
	 * Constant value for fishnet state lost
	 */
	private static final int STATE_LOST = 4;

	/*
	 * Support creating and retrieving FishnetState entities
	 */

	private Integer id;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private FishnetState fishnetState;

	public FishnetState getFishnetState() {
		return this.fishnetState;
	}

	public void setFishnetState(FishnetState fishnetState) {
		this.fishnetState = fishnetState;
	}

	@Inject
	private Conversation conversation;

	@PersistenceContext(unitName = "ghostNetFishing-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	/**
	 * Creates a new session for conversation.
	 * @return redirect to FishnetState create mask.
	 */
	public String create() {

		this.conversation.begin();
		this.conversation.setTimeout(1800000L);
		return "create?faces-redirect=true";
	}

	/**
	 * Check if a conversation is already started, when it is started, reload the page.
	 * If not, start a new conversation.
	 */
	public void retrieve() {

		if (FacesContext.getCurrentInstance().isPostback()) {
			return;
		}

		if (this.conversation.isTransient()) {
			this.conversation.begin();
			this.conversation.setTimeout(1800000L);
		}

		if (this.id == null) {
			this.fishnetState = this.example;
		} else {
			this.fishnetState = findById(getId());
		}
	}

	/**
	 * Used for search FishnetState by id.
	 * @param id.
	 * @return FishnetState with wanted id.
	 */
	public FishnetState findById(Integer id) {

		return this.entityManager.find(FishnetState.class, id);
	}

	/**
	 * Creates new FishnetState object if doesn't exists.
	 * if exists do an update on the exists entity.
	 * @return a redirecting path or null on error.
	 */
	public String update() {
		this.conversation.end();

		try {
			if (this.id == null) {
				this.entityManager.persist(this.fishnetState);
				return "search?faces-redirect=true";
			} else {
				this.entityManager.merge(this.fishnetState);
				return "view?faces-redirect=true&id="
				+ this.fishnetState.getId();
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	/**
	 * Deletes current FishnetState object and references to relevant objects.
	 * @return a redirecting path or null on error.
	 */
	public String delete() {
		this.conversation.end();

		try {
			FishnetState deletableEntity = findById(getId());
			Iterator<Fishnet> iterFishnets = deletableEntity.getFishnets()
					.iterator();
			for (; iterFishnets.hasNext();) {
				Fishnet nextInFishnets = iterFishnets.next();
				nextInFishnets.setFishnetState(null);
				iterFishnets.remove();
				this.entityManager.merge(nextInFishnets);
			}
			this.entityManager.remove(deletableEntity);
			this.entityManager.flush();
			return "search?faces-redirect=true";
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	/*
	 * Support searching FishnetState entities with pagination.
	 */

	/**
	 * Page position.
	 */
	private int page;
	
	/**
	 * Page counter.
	 */
	private long count;
	
	/**
	 * List of FishnetState objects.
	 */
	private List<FishnetState> pageItems;

	/**
	 * Used as search parameter in search view.
	 */
	private FishnetState example = new FishnetState();

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return 10;
	}

	public FishnetState getExample() {
		return this.example;
	}

	public void setExample(FishnetState example) {
		this.example = example;
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
	 * When the user paginates through the page, it retrieves data for display a list of items on the page.
	 */
	public void paginate() {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();

		// Populate this.count

		CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
		Root<FishnetState> root = countCriteria.from(FishnetState.class);
		countCriteria = countCriteria.select(builder.count(root)).where(
				getSearchPredicates(root));
		this.count = this.entityManager.createQuery(countCriteria)
				.getSingleResult();

		// Populate this.pageItems

		CriteriaQuery<FishnetState> criteria = builder
				.createQuery(FishnetState.class);
		root = criteria.from(FishnetState.class);
		TypedQuery<FishnetState> query = this.entityManager
				.createQuery(criteria.select(root).where(
						getSearchPredicates(root)));
		query.setFirstResult(this.page * getPageSize()).setMaxResults(
				getPageSize());
		this.pageItems = query.getResultList();
	}

	/**
	 * Returns an array with search predicates as condition for paginate method.
	 * @param root as FishnetState object.
	 * @return array of search predicates.
	 */
	private Predicate[] getSearchPredicates(Root<FishnetState> root) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		String description = this.example.getDescription();
		if (description != null && !"".equals(description)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("description")),
					'%' + description.toLowerCase() + '%'));
		}
		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	public List<FishnetState> getPageItems() {
		return this.pageItems;
	}

	public long getCount() {
		return this.count;
	}

	/*
	 * Support listing and POSTing back FishnetState entities (e.g. from inside
	 * an HtmlSelectOneMenu)
	 */
	
	/**
	 * Get all FishnetState objects as list.
	 * @return a list of FishnetState objects.
	 */
	public List<FishnetState> getAll() {
		return FishnetStateBean.fetchAll(this.entityManager);
	}
	
	public static List<FishnetState> fetchAll(EntityManager manager) {

		CriteriaQuery<FishnetState> criteria = manager
				.getCriteriaBuilder().createQuery(FishnetState.class);
		return manager.createQuery(
				criteria.select(criteria.from(FishnetState.class)))
				.getResultList();
	}
	
	/**
	 * Lists of possible destination states for given currentstate and persontype  
	 * this is the lowtec variant, realized with constant values, better way is to realize it with the checks in the databases
	 * 
	 * @param manager is the entity manager provided by the client.
	 * @param currentState is the source state (is required).
	 * @param personType is the person who wants to change the state (is required).
	 * @return list for FishnetStates.
	 * @throws IllegalArgumentException by an error.
	 */
	public static List<FishnetState> fetchAllPossibleDestinationStates(EntityManager manager, FishnetState currentState, PersonType personType) throws IllegalArgumentException{

		if(manager == null || currentState == null || personType == null){
			throw new IllegalArgumentException("method parameters must not be null");
		}
		
		List<FishnetState> valideDestinationStates = new ArrayList<>();
		
		for(FishnetState state : FishnetStateBean.fetchAll(manager)) {

			switch(state.getId()){

			case STATE_REPORTED:
				break;

			case STATE_RECOVERY_PENDING:
				if(personType.getId() ==  PERSON_TYPE_RECOVERER && currentState.getId() == STATE_REPORTED){
					valideDestinationStates.add(state);
				}
				break;

			case STATE_RECOVERED:
				if(personType.getId() ==  PERSON_TYPE_RECOVERER && currentState.getId() == STATE_RECOVERY_PENDING){
					valideDestinationStates.add(state);
				}
				break;
				
			case STATE_LOST:	
				if(personType.getId() !=  PERSON_TYPE_ANONYMOUS){
					valideDestinationStates.add(state);
				}
				break;
				
			default:
				//DO nothing
			}
		}

		return valideDestinationStates;
	}

	/**
	 * Retrieve all FishnetState with the flag relevant for recover.
	 * @param entityManager.
	 * @return all FishnetState which are relevant to display, in case of recovery.
	 */
	public static List<FishnetState> getAllRelevantForRecovery(EntityManager entityManager){

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<FishnetState> criteria = builder.createQuery(FishnetState.class);
		Root<FishnetState> root = criteria.from(FishnetState.class);

		return entityManager.createQuery(
				criteria.select(root).where(builder.isTrue(
						root.<Boolean>get("relevantForRecoveryFlag")))).getResultList();
	}

	@Resource
	private SessionContext sessionContext;

	/**
	 * Converts an object to a String for displaying it as dataset.
	 * @return object as string.
	 */
	public Converter getConverter() {

		final FishnetStateBean ejbProxy = this.sessionContext
				.getBusinessObject(FishnetStateBean.class);

		return new Converter() {

			@Override
			public Object getAsObject(FacesContext context,
					UIComponent component, String value) {

				return ejbProxy.findById(Integer.valueOf(value));
			}

			@Override
			public String getAsString(FacesContext context,
					UIComponent component, Object value) {

				if (value == null) {
					return "";
				}

				return String.valueOf(((FishnetState) value).getId());
			}
		};
	}

	/*
	 * Support adding children to bidirectional, one-to-many tables
	 */

	private FishnetState add = new FishnetState();

	public FishnetState getAdd() {
		return this.add;
	}

	public FishnetState getAdded() {
		FishnetState added = this.add;
		this.add = new FishnetState();
		return added;
	}
}
