package magicdraw;

import sparqldesignruleplugin.SPARQLDesignRulePlugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.runtime.*;

public class RunMagicDraw {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SPARQLDesignRulePlugin sparqlDesignRulePlugin = new SPARQLDesignRulePlugin();			
			Application app = Application.getInstance();
			app.start(true, false, false, args, null);
			sparqlDesignRulePlugin.init();
		} catch (ApplicationExitedException e) {
			e.printStackTrace();
		}

	}

}