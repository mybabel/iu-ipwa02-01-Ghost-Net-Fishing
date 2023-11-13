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
import org.el.ghostnetfishing.model.Person;

/**
 * Backing bean for Fishnet entities.
 * <p/>
 * This class provides CRUD functionality for all Fishnet entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class FishnetBean implements Serializable {

	/**
	 * Uniquely identify the version of a serializable.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constant value for fishnet state reported.
	 */
    private final int FISHNET_REPORTED_STATE = 1;
    
    /**
     * Current person id.
     */
    private int personId;

	/*
	 * Support creating and retrieving Fishnet entities.
	 */

    /*
     * Current fishnet id.
     */
	private Integer id;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Fishnet fishnet;

	public Fishnet getFishnet() {
		return this.fishnet;
	}

	public void setFishnet(Fishnet fishnet) {
		this.fishnet = fishnet;
	}

	public int getPersonId() {
		return personId;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}

	@Inject
	private Conversation conversation;

	@PersistenceContext(unitName = "ghostNetFishing-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	/**
	 * Creates a new session for conversation.
	 * @return redirect to fishnet create mask.
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
			this.fishnet = this.example;
		} else {
			this.fishnet = findById(getId());
		}
	}

	/**
	 * Used for searching by id.
	 * @param id.
	 * @return searching id.
	 */
	public Fishnet findById(Integer id) {
		return this.entityManager.find(Fishnet.class, id);
	}

	/*
	 * Support updating and deleting Fishnet entities.
	 */

	/**
	 * Creates new Fishnet object if doesn't exists.
	 * if exists do an update on the exists entity.
	 * @return a redirecting path or null on error.
	 */
	public String update() {
		this.conversation.end();
		Fishnet fishnet = this.fishnet;
		
		// creating a fishnet with the fishnetstate reported
		fishnet.setFishnetState(new FishnetState(FISHNET_REPORTED_STATE));
		
		// add the reporting person id to the fishnet
		fishnet.setPersonBySalvagingPersonId(entityManager.find(Person.class, personId));
		
		try {
			if (this.id == null) {
				this.entityManager.persist(fishnet);
				return "/index?faces-redirect=true";
			} else {
				this.entityManager.merge(fishnet);
				return "create?faces-redirect=true&id=" + fishnet.getId();
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	/**
	 * Deletes current Fishnet object and references to relevant objects.
	 * @return a redirecting path or null on error.
	 */
	public String delete() {
		this.conversation.end();

		try {
			Fishnet deletableEntity = findById(getId());
			FishnetState fishnetState = deletableEntity.getFishnetState();
			fishnetState.getFishnets().remove(deletableEntity);
			deletableEntity.setFishnetState(null);
			this.entityManager.merge(fishnetState);
			Person personBySalvagingPersonId = deletableEntity
					.getPersonBySalvagingPersonId();
			personBySalvagingPersonId.getFishnetsForSalvagingPersonId().remove(
					deletableEntity);
			deletableEntity.setPersonBySalvagingPersonId(null);
			this.entityManager.merge(personBySalvagingPersonId);
			Person personByRecoveryMessagingPersonId = deletableEntity
					.getPersonByRecoveryMessagingPersonId();
			personByRecoveryMessagingPersonId
					.getFishnetsForRecoveryMessagingPersonId().remove(
							deletableEntity);
			deletableEntity.setPersonByRecoveryMessagingPersonId(null);
			this.entityManager.merge(personByRecoveryMessagingPersonId);
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
	 * Support searching Fishnet entities with pagination.
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
	 * List of fishnet objects.
	 */
	private List<Fishnet> pageItems;

	/**
	 * Used as search parameter in search view.
	 */
	private Fishnet example = new Fishnet();

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
		Root<Fishnet> root = countCriteria.from(Fishnet.class);
		countCriteria = countCriteria.select(builder.count(root)).where(
				getSearchPredicates(root));
		this.count = this.entityManager.createQuery(countCriteria)
				.getSingleResult();

		// Populate this.pageItems

		CriteriaQuery<Fishnet> criteria = builder.createQuery(Fishnet.class);
		root = criteria.from(Fishnet.class);
		TypedQuery<Fishnet> query = this.entityManager.createQuery(criteria
				.select(root).where(getSearchPredicates(root)));
		query.setFirstResult(this.page * getPageSize()).setMaxResults(
				getPageSize());
		this.pageItems = query.getResultList();
	}

	/**
	 * Returns an array with search predicates as condition for paginate method.
	 * @param root as Fishnet object.
	 * @return array of search predicates.
	 */
	private Predicate[] getSearchPredicates(Root<Fishnet> root) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		FishnetState fishnetState = this.example.getFishnetState();
		if (fishnetState != null) {
			predicatesList.add(builder.equal(root.get("fishnetState"),
					fishnetState));
		}
		Person personBySalvagingPersonId = this.example
				.getPersonBySalvagingPersonId();
		if (personBySalvagingPersonId != null) {
			predicatesList.add(builder.equal(
					root.get("personBySalvagingPersonId"),
					personBySalvagingPersonId));
		}
		Person personByRecoveryMessagingPersonId = this.example
				.getPersonByRecoveryMessagingPersonId();
		if (personByRecoveryMessagingPersonId != null) {
			predicatesList.add(builder.equal(
					root.get("personByRecoveryMessagingPersonId"),
					personByRecoveryMessagingPersonId));
		}
		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	public List<Fishnet> getPageItems() {
		return this.pageItems;
	}

	public long getCount() {
		return this.count;
	}

	/*
	 * Support listing and POSTing back Fishnet entities (e.g. from inside an
	 * HtmlSelectOneMenu)
	 */

	/**
	 * Get all Fishnet objects as list.
	 * @return a list of Fishnet objects.
	 */
	public List<Fishnet> getAll() {

		CriteriaQuery<Fishnet> criteria = this.entityManager
				.getCriteriaBuilder().createQuery(Fishnet.class);
		return this.entityManager.createQuery(
				criteria.select(criteria.from(Fishnet.class))).getResultList();
	}

	@Resource
	private SessionContext sessionContext;

	/**
	 * Converts an object to a String for displaying it as dataset.
	 * @return object as string.
	 */
	public Converter getConverter() {

		final FishnetBean ejbProxy = this.sessionContext
				.getBusinessObject(FishnetBean.class);

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
				return String.valueOf(((Fishnet) value).getId());
			}
		};
	}

	/*
	 * Support adding children to bidirectional, one-to-many tables
	 */
	
	private Fishnet add = new Fishnet();

	public Fishnet getAdd() {
		return this.add;
	}

	public Fishnet getAdded() {
		Fishnet added = this.add;
		this.add = new Fishnet();
		return added;
	}
}
