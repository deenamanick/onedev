package io.onedev.server.manager.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.criterion.Restrictions;

import io.onedev.launcher.loader.ListenerRegistry;
import io.onedev.server.event.issue.IssueChanged;
import io.onedev.server.manager.IssueChangeManager;
import io.onedev.server.manager.IssueFieldManager;
import io.onedev.server.manager.IssueManager;
import io.onedev.server.model.Issue;
import io.onedev.server.model.IssueChange;
import io.onedev.server.model.IssueField;
import io.onedev.server.model.support.LastActivity;
import io.onedev.server.model.support.issue.PromptedField;
import io.onedev.server.model.support.issue.changedata.DescriptionChangeData;
import io.onedev.server.model.support.issue.changedata.FieldChangeData;
import io.onedev.server.model.support.issue.changedata.StateChangeData;
import io.onedev.server.model.support.issue.changedata.TitleChangeData;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.AbstractEntityManager;
import io.onedev.server.persistence.dao.Dao;
import io.onedev.server.persistence.dao.EntityCriteria;
import io.onedev.server.security.SecurityUtils;

@Singleton
public class DefaultIssueChangeManager extends AbstractEntityManager<IssueChange>
		implements IssueChangeManager {

	private final IssueManager issueManager;
	
	private final IssueFieldManager issueFieldManager;
	
	private final ListenerRegistry listenerRegistry;
	
	@Inject
	public DefaultIssueChangeManager(Dao dao, IssueManager issueManager,  
			IssueFieldManager issueFieldManager, ListenerRegistry listenerRegistry) {
		super(dao);
		this.issueManager = issueManager;
		this.issueFieldManager = issueFieldManager;
		this.listenerRegistry = listenerRegistry;
	}

	@Transactional
	@Override
	public void changeTitle(Issue issue, String prevTitle) {
		LastActivity lastActivity = new LastActivity();
		lastActivity.setAction("changed title");
		lastActivity.setDate(new Date());
		lastActivity.setUser(SecurityUtils.getUser());
		issueManager.save(issue);
		
		IssueChange change = new IssueChange();
		change.setIssue(issue);
		change.setDate(new Date());
		change.setUser(SecurityUtils.getUser());
		change.setData(new TitleChangeData(prevTitle, issue.getTitle()));
		save(change);
		
		listenerRegistry.post(new IssueChanged(change));
	}
	
	@Transactional
	@Override
	public void changeDescription(Issue issue, String prevDescription) {
		LastActivity lastActivity = new LastActivity();
		lastActivity.setAction("changed description");
		lastActivity.setDate(new Date());
		lastActivity.setUser(SecurityUtils.getUser());
		issueManager.save(issue);
		
		IssueChange change = new IssueChange();
		change.setIssue(issue);
		change.setDate(new Date());
		change.setUser(SecurityUtils.getUser());
		change.setData(new DescriptionChangeData(prevDescription, issue.getDescription()));
		save(change);
		
		listenerRegistry.post(new IssueChanged(change));
	}

	@Transactional
	@Override
	public void changeFields(Issue issue, Serializable fieldBean, Map<String, PromptedField> prevFields, 
			Collection<String> promptedFields) {
		LastActivity lastActivity = new LastActivity();
		lastActivity.setAction("changed custom fields");
		lastActivity.setDate(new Date());
		lastActivity.setUser(SecurityUtils.getUser());
		issueManager.save(issue);
		
		issueFieldManager.writeFields(issue, fieldBean, promptedFields);
		IssueChange change = new IssueChange();
		change.setIssue(issue);
		change.setDate(new Date());
		change.setUser(SecurityUtils.getUser());
		getSession().flush();
		
		EntityCriteria<IssueField> criteria = EntityCriteria.of(IssueField.class);
		criteria.add(Restrictions.eq("issue", issue));
		issue.setFields(dao.findAll(criteria));
		
		change.setData(new FieldChangeData(prevFields, issue.getPromptedFields()));
		save(change);
		
		listenerRegistry.post(new IssueChanged(change));
	}
	
	
	@Transactional
	@Override
	public void changeState(Issue issue, Serializable fieldBean, @Nullable String commentContent,
			Map<String, PromptedField> prevFields, Collection<String> promptedFields) {
		long time = System.currentTimeMillis();
		LastActivity lastActivity = new LastActivity();
		lastActivity.setAction("changed state to \"" + issue.getState() + "\"");
		lastActivity.setDate(new Date(time));
		lastActivity.setUser(SecurityUtils.getUser());
		issueManager.save(issue);
		
		issueFieldManager.writeFields(issue, fieldBean, promptedFields);
		IssueChange change = new IssueChange();
		change.setIssue(issue);
		change.setDate(new Date(time));
		change.setUser(SecurityUtils.getUser());
		
		getSession().flush();
		
		EntityCriteria<IssueField> criteria = EntityCriteria.of(IssueField.class);
		criteria.add(Restrictions.eq("issue", issue));
		issue.setFields(dao.findAll(criteria));
		
		change.setData(new StateChangeData(issue.getState(), prevFields, issue.getPromptedFields(), commentContent));
		
		save(change);
		
		listenerRegistry.post(new IssueChanged(change));
	}
	
}