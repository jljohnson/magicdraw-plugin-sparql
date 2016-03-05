package sparqldesignruleplugin;

import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;

public class SPARQLDesignRulePlugin extends com.nomagic.magicdraw.plugins.Plugin {
	public void init() {
		ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
        SPARQLDesignRuleConfigurator configurator = new SPARQLDesignRuleConfigurator(); 
        manager.addContainmentBrowserContextConfigurator( configurator ); 
	}

	public boolean close() {
		return true;
	}

	public boolean isSupported() {
		// plugin can check here for specific conditions
		// if false is returned plugin is not loaded.
		return true;
	}
}
