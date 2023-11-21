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

import org.el.ghostnetfishing.model.Person;
import org.el.ghostnetfishing.model.PersonType;

import java.util.Iterator;

/**
 * Backing bean for PersonType entities.
 * <p/>
 * This class provides CRUD functionality for all PersonType entities. It
 * focuses purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt>
 * for state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class PersonTypeBean implements Serializable {

	/**
	 * uniquely identify the version of a serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Current person type id
	 */
	private Integer id;

	/*
	 * Support creating and retrieving PersonType entities
	 */

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private PersonType personType;

	public PersonType getPersonType() {
		return this.personType;
	}

	public void setPersonType(PersonType personType) {
		this.personType = personType;
	}

	@Inject
	private Conversation conversation;

	@PersistenceContext(unitName = "ghostNetFishing-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	/**
	 * Creates a new session for conversation.
	 * @return redirect to PersonType create mask.
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
			this.personType = this.example;
		} else {
			this.personType = findById(getId());
		}
	}

	/**
	 * Used for search PersonType by id.
	 * @param id.
	 * @return PersonType with wanted id.
	 */
	public PersonType findById(Integer id) {

		return this.entityManager.find(PersonType.class, id);
	}

	/*
	 * Support updating and deleting PersonType entities
	 */

	/**
	 * Creates new PersonType object if doesn't exists.
	 * if exists do an update on the exists entity.
	 * @return a redirecting path or null on error.
	 */
	public String update() {
		this.conversation.end();

		try {
			if (this.id == null) {
				this.entityManager.persist(this.personType);
				return "search?faces-redirect=true";
			} else {
				this.entityManager.merge(this.personType);
				return "view?faces-redirect=true&id=" + this.personType.getId();
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}

	/**
	 * Deletes current PersonType object and references to relevant objects.
	 * @return a redirecting path or null on error.
	 */
	public String delete() {
		this.conversation.end();

		try {
			PersonType deletableEntity = findById(getId());
			Iterator<Person> iterPersons = deletableEntity.getPersons()
					.iterator();
			for (; iterPersons.hasNext();) {
				Person nextInPersons = iterPersons.next();
				nextInPersons.setPersonType(null);
				iterPersons.remove();
				this.entityManager.merge(nextInPersons);
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
	 * Support searching PersonType entities with pagination.
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
	 * List of PersonType objects.
	 */
	private List<PersonType> pageItems;

	/**
	 * Used as search parameter in search view.
	 */
	private PersonType example = new PersonType();

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return 10;
	}

	public PersonType getExample() {
		return this.example;
	}

	public void setExample(PersonType example) {
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
		Root<PersonType> root = countCriteria.from(PersonType.class);
		countCriteria = countCriteria.select(builder.count(root)).where(
				getSearchPredicates(root));
		this.count = this.entityManager.createQuery(countCriteria)
				.getSingleResult();

		// Populate this.pageItems

		CriteriaQuery<PersonType> criteria = builder
				.createQuery(PersonType.class);
		root = criteria.from(PersonType.class);
		TypedQuery<PersonType> query = this.entityManager.createQuery(criteria
				.select(root).where(getSearchPredicates(root)));
		query.setFirstResult(this.page * getPageSize()).setMaxResults(
				getPageSize());
		this.pageItems = query.getResultList();
	}

	/**
	 * Returns an array with search predicates as condition for paginate method.
	 * @param root as PersonType object.
	 * @return array of search predicates.
	 */
	private Predicate[] getSearchPredicates(Root<PersonType> root) {

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

	public List<PersonType> getPageItems() {
		return this.pageItems;
	}

	public long getCount() {
		return this.count;
	}

	/*
	 * Support listing and POSTing back PersonType entities (e.g. from inside an
	 * HtmlSelectOneMenu)
	 */

	/**
	 * Get all PersonType objects as list.
	 * @return a list of PersonType objects.
	 */
	public List<PersonType> getAll() {

		CriteriaQuery<PersonType> criteria = this.entityManager
				.getCriteriaBuilder().createQuery(PersonType.class);
		return this.entityManager.createQuery(
				criteria.select(criteria.from(PersonType.class)))
				.getResultList();
	}

	@Resource
	private SessionContext sessionContext;

	/**
	 * Converts an object to a String for displaying it as dataset.
	 * @return object as string.
	 */
	public Converter getConverter() {

		final PersonTypeBean ejbProxy = this.sessionContext
				.getBusinessObject(PersonTypeBean.class);

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

				return String.valueOf(((PersonType) value).getId());
			}
		};
	}

	/*
	 * Support adding children to bidirectional, one-to-many tables
	 */

	private PersonType add = new PersonType();

	public PersonType getAdd() {
		return this.add;
	}

	public PersonType getAdded() {
		PersonType added = this.add;
		this.add = new PersonType();
		return added;
	}
}
