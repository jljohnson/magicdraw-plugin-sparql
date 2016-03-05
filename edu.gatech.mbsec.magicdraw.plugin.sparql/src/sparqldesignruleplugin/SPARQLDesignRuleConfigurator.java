package sparqldesignruleplugin;

import java.util.List;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.*;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;

public class SPARQLDesignRuleConfigurator implements BrowserContextAMConfigurator, AMConfigurator
{

	/**
	 * Action which should be added to the tree.
	 */
	private MDActionsCategory cat = null;
	
  
    
//    private DefaultBrowserAction sparqlDesignRuleAction     = new SPARQLDesignRuleAction();

    
   
	
	/**
	 * Creates configurator for adding given action.
	 * @param action action to be added to manager.
	 */
	public SPARQLDesignRuleConfigurator()	{
	}

	
	public void configure(ActionsManager mngr, Tree tree) 	{
		if(tree.getSelectedNode() == null) { 
			return;
		}
		ActionsCategory  cat = new ActionsCategory(null,null);

		if(Application.getInstance().getProject() == null) { 
			return;
		}
		
        cat = new MDActionsCategory("Execute SPARQL Design Rule","Execute SPARQL Design Rule");
		
        // Not nested means group (usually separated by some menu separator).
        cat.setNested(false);
        
		Object userObject = tree.getSelectedNode().getUserObject();		
		
		// only add action to constraints
		if (userObject instanceof Constraint) { 
			cat.addAction(new SPARQLDesignRuleAction((Constraint)userObject));			
			mngr.addCategory(cat);
		} 

	}

	/**
	 * @see com.nomagic.actions.AMConfigurator#configure(com.nomagic.actions.ActionsManager)
	 */
	public void configure(ActionsManager mngr)	{
		// adding action separator
		mngr.addCategory(cat);
	}

	public int getPriority()
	{
		return AMConfigurator.MEDIUM_PRIORITY;
	}
}