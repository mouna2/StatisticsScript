import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger;
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.jql.builder.JqlQueryBuilder
import com.atlassian.jira.web.bean.PagerFilter
import java.text.SimpleDateFormat
import com.atlassian.jira.project.Project
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.DelegatingApplicationUser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.issue.search.SearchException

def log = Logger.getLogger("atlassian-jira.log")

List<Project> prList = ComponentAccessor.getProjectManager().getProjectObjects()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchProvider = ComponentAccessor.getComponent(SearchProvider)
def issueManager = ComponentAccessor.getIssueManager()
def projectManager = ComponentAccessor.projectManager
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)

log.warn("Project category|Project name|Last Date Updated| Total issues|Number of issues updated in the last year|Project Lead Display Name|Project Lead Key|Number of Admins|Admin List")
def searchService = ComponentAccessor.getOSGiComponentInstanceOfType(SearchService.class)
ApplicationUser	 user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()

def emptyList = []

for(Project myproject: prList){
    def lastUpdatedDate = new Date(Long.MIN_VALUE)
    def builder = JqlQueryBuilder.newBuilder()
    builder.where().project(myproject.id)
    def query = builder.buildQuery()
    def results = searchService.search(user, query, PagerFilter.getUnlimitedFilter())
    int issuesUpdatedInLastYear=0; 
    def lastUpdated =""
    if (results.getTotal() == 0) {
        emptyList.add(myproject.getName())
        log.warn("Empty project")
    }
    else {
       
        def i = 0
        for (Issue issue : results.getResults())
        {
             lastUpdated = issue.getUpdated()
            if (i == 0 || lastUpdated > lastUpdatedDate)
                lastUpdatedDate = lastUpdated
            i++
        }
        log.warn("project key: "+myproject.getKey()+" "+myproject.getName())
        def query2 = ""
        try{
         query2 = jqlQueryParser.parseQuery("project = \""+myproject.getName()+"\" and updated > 2021-08-25 ")

        }catch(Exception e){
            log.warn("MounaException "+e)
        }

        def search = searchService.search(user, query2, PagerFilter.getUnlimitedFilter())
        log.warn("Updated issues in the last year: ${search.total}"+" project "+myproject.getKey()+"/"+results.getTotal() )
        issuesUpdatedInLastYear=search.total
    


    
    }
    
    def projectCategory=""
    if(myproject.getProjectCategory()!=null){
        projectCategory=myproject.getProjectCategory().getName()
    }
       ( admins, adminSize)= getAdmins(projectRoleManager, myproject, user, projectManager)
        def lastDate= new SimpleDateFormat("dd/MMM/yy").format(lastUpdatedDate)
        log.warn("bb|"+projectCategory+"|"+myproject.getName()+"|"+lastDate + "|"+results.getResults().size()+ "|"+issuesUpdatedInLastYear+ "|"+ myproject.getProjectLead().getDisplayName()+ "|"+ myproject.getProjectLead().getKey() +"|"+adminSize+"|"+admins)
       
    
}
     log.warn("Empty projects: "+ emptyList.size()+"/"+prList.size() +" "+emptyList)


def getAdmins(ProjectRoleManager projectRoleManager, Project myproject, ApplicationUser user,  ProjectManager projectManager){


def adminProjects = ""

ProjectRole projectRole = projectRoleManager.getProjectRole("Administrators")

 def projectRoles = projectRoleManager.getProjectRoles(user, myproject)
  Project project = projectManager.getProjectObjByKey(myproject.getKey())
   
    if (project) {
        ProjectRoleActors actors = projectRoleManager.getProjectRoleActors(projectRole, project)
        adminProjects=actors.getUsers()*.name
        adminSize=actors.getUsers().size()
        log.warn("ADMIN SIZE "+adminSize)
    }
    return [adminProjects, adminSize]

}

