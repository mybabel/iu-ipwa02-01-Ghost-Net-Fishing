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
import org.el.ghostnetfishing.model.Person;
import org.el.ghostnetfishing.model.PersonType;

import java.util.Iterator;

/**
 * Backing bean for Person entities.
 * <p/>
 * This class provides CRUD functionality for all Person entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Named
@Stateful
@ConversationScoped
public class PersonBean implements Serializable {

	/**
	 * Uniquely identify the version of a serializable
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Flag for telephone number is required, is settled by default true
	 */
	private boolean telephoneNumberRequiredFlag = true;
	
	/**
	 * Initialization of person type, is settled by default to 1
	 */
	private int personType = 1;

	
	/**
	 * Constant value for person type anonymous reporter 
	 */
	private static final int PERSON_TYPE_ANONYMOUS_REPORTER = 1;
	
	/**
	 * Constant value for person type reporter 
	 */
	private static final int PERSON_TYPE_REPORTER = 2;
	
	/**
	 * Constant value for person type recoverer 
	 */
	private static final int PERSON_TYPE_RECOVER = 3;
	
	/*
	 * Support creating and retrieving Person entities
	 */

	public boolean isTelephoneNumberRequiredFlag() {
		return telephoneNumberRequiredFlag;
	}

	public void setTelephoneNumberRequiredFlag(boolean telephoneNumberRequiredFlag) {
		this.telephoneNumberRequiredFlag = telephoneNumberRequiredFlag;
	}	
		
	/**
	 * Current person id
	 */
	private Integer id;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	private Person person;

	public Person getPerson() {
		return this.person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}
	
	public int getPersonType() {
		return personType;
	}

	public void setPersonType(int personType) {
		this.personType = personType;
	}
	
	/**
	 * 
	 * @param personType
	 */
	private void setPersonTypeSelected(int personType) {
		
		int type = personType;
		
		switch (personType){
		
		case PERSON_TYPE_ANONYMOUS_REPORTER:
			type = PERSON_TYPE_ANONYMOUS_REPORTER;
			break;
		case PERSON_TYPE_REPORTER:
			type = PERSON_TYPE_REPORTER;
			break;
		case PERSON_TYPE_RECOVER:	
			type = PERSON_TYPE_RECOVER;
			break;
		default:
			System.out.println("Wrong personType as parameter");
			break;
		}
		getPerson().setPersonType(this.entityManager.find(PersonType.class, type));
	}

	@Inject
	private Conversation conversation;

	@PersistenceContext(unitName = "ghostNetFishing-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager entityManager;

	/**
	 * Creates a new session for conversation.
	 * @return redirect to Person create mask.
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
			this.person = this.example;
			
			// disabled telephoneNumberField when user is anonymous
			if(getPersonType() == PERSON_TYPE_ANONYMOUS_REPORTER){
				setTelephoneNumberRequiredFlag(false);
			}
			else {
				setTelephoneNumberRequiredFlag(true);
			}
			setPersonTypeSelected(getPersonType());
			
		} else {
			this.person = findById(getId());
		}
	}

	/**
	 * Used for search Person by id.
	 * @param id.
	 * @return Person with wanted id.
	 */
	public Person findById(Integer id) {

		return this.entityManager.find(Person.class, id);
	}

	/*
	 * Support updating and deleting Person entities
	 */

	
	/**
	 * Creates new Person object if doesn't exists.
	 * if exists do an update on the exists entity.
	 * @return a redirecting path or null on error.
	 */
	public String update() {
		this.conversation.end();
		
		// allocates current person
		Person person = this.person;
		
		// initialize persontype with 1
		int personType = 1;
		
		// allocates current persontype
		if(getPersonType() == PERSON_TYPE_REPORTER){
			personType = PERSON_TYPE_REPORTER;
		}
		else if(getPersonType() == PERSON_TYPE_ANONYMOUS_REPORTER){
			personType = PERSON_TYPE_ANONYMOUS_REPORTER;
		}
		else if(getPersonType() == PERSON_TYPE_RECOVER){
			personType = PERSON_TYPE_RECOVER;
		}
		else{
			System.out.println("Wrong usertype as parameter");
		}
		
		// Set the persontype for the new person object
		person.setPersonType(new PersonType(personType));

		try {
			if (this.id == null) {
				this.entityManager.persist(this.person);
				
				// if the persontype is a recoverer, redirect to the index.xhtml
				if(personType == PERSON_TYPE_RECOVER){
					return "/index.xhtml";
				}
				else {
					// if the persontype is not a recoverer, redirect to the mask for creating a new fishnet
					return "/fishnet/create.xhtml?faces-redirect=true&personId=" + this.person.getId();
				}
			} else {
				this.entityManager.merge(this.person);
				return "create?faces-redirect=true&id=" + this.person.getId();
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Deletes current Person object and references to relevant objects.
	 * @return a redirecting path or null on error.
	 */
	public String delete() {
		this.conversation.end();

		try {
			Person deletableEntity = findById(getId());
			PersonType personType = deletableEntity.getPersonType();
			personType.getPersons().remove(deletableEntity);
			deletableEntity.setPersonType(null);
			this.entityManager.merge(personType);
			Iterator<Fishnet> iterFishnetsForSalvagingPersonId = deletableEntity
					.getFishnetsForSalvagingPersonId().iterator();
			for (; iterFishnetsForSalvagingPersonId.hasNext();) {
				Fishnet nextInFishnetsForSalvagingPersonId = iterFishnetsForSalvagingPersonId
						.next();
				nextInFishnetsForSalvagingPersonId
				.setPersonBySalvagingPersonId(null);
				iterFishnetsForSalvagingPersonId.remove();
				this.entityManager.merge(nextInFishnetsForSalvagingPersonId);
			}
			Iterator<Fishnet> iterFishnetsForRecoveryMessagingPersonId = deletableEntity
					.getFishnetsForRecoveryMessagingPersonId().iterator();
			for (; iterFishnetsForRecoveryMessagingPersonId.hasNext();) {
				Fishnet nextInFishnetsForRecoveryMessagingPersonId = iterFishnetsForRecoveryMessagingPersonId
						.next();
				nextInFishnetsForRecoveryMessagingPersonId
				.setPersonByRecoveryMessagingPersonId(null);
				iterFishnetsForRecoveryMessagingPersonId.remove();
				this.entityManager
				.merge(nextInFishnetsForRecoveryMessagingPersonId);
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
	 * Support searching Person entities with pagination.
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
	 * List of person objects.
	 */
	private List<Person> pageItems;

	/**
	 * Used as search parameter in search view.
	 */
	private Person example = new Person();

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return 10;
	}

	public Person getExample() {
		return this.example;
	}

	public void setExample(Person example) {
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
		Root<Person> root = countCriteria.from(Person.class);
		countCriteria = countCriteria.select(builder.count(root)).where(
				getSearchPredicates(root));
		this.count = this.entityManager.createQuery(countCriteria)
				.getSingleResult();

		// Populate this.pageItems

		CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
		root = criteria.from(Person.class);
		TypedQuery<Person> query = this.entityManager.createQuery(criteria
				.select(root).where(getSearchPredicates(root)));
		query.setFirstResult(this.page * getPageSize()).setMaxResults(
				getPageSize());
		this.pageItems = query.getResultList();
	}

	/**
	 * Returns an array with search predicates as condition for paginate method.
	 * @param root as Person object.
	 * @return array of search predicates.
	 */
	private Predicate[] getSearchPredicates(Root<Person> root) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		PersonType personType = this.example.getPersonType();
		if (personType != null) {
			predicatesList
			.add(builder.equal(root.get("personType"), personType));
		}
		String name = this.example.getName();
		if (name != null && !"".equals(name)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("name")),
					'%' + name.toLowerCase() + '%'));
		}
		String telephoneNumber = this.example.getTelephoneNumber();
		if (telephoneNumber != null && !"".equals(telephoneNumber)) {
			predicatesList.add(builder.like(
					builder.lower(root.<String> get("telephoneNumber")),
					'%' + telephoneNumber.toLowerCase() + '%'));
		}
		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	public List<Person> getPageItems() {
		return this.pageItems;
	}

	public long getCount() {
		return this.count;
	}

	/*
	 * Support listing and POSTing back Person entities (e.g. from inside an
	 * HtmlSelectOneMenu)
	 */
	
	/**
	 * Get all Person objects as list.
	 * @return a list of Person objects.
	 */
	public List<Person> getAll() {

		CriteriaQuery<Person> criteria = this.entityManager
				.getCriteriaBuilder().createQuery(Person.class);
		return this.entityManager.createQuery(
				criteria.select(criteria.from(Person.class))).getResultList();
	}

	@Resource
	private SessionContext sessionContext;

	/**
	 * Converts an object to a String for displaying it as dataset.
	 * @return object as string.
	 */
	public Converter getConverter() {

		final PersonBean ejbProxy = this.sessionContext
				.getBusinessObject(PersonBean.class);

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

				return String.valueOf(((Person) value).getId());
			}
		};
	}

	/**
	 * Support adding children to bidirectional, one-to-many tables
	 */
	
	private Person add = new Person();

	public Person getAdd() {
		return this.add;
	}

	public Person getAdded() {
		Person added = this.add;
		this.add = new Person();
		return added;
	}
}
