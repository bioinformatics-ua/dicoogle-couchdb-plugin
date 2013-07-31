/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.PluginBase;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import org.ektorp.impl.StdCouchDbInstance;

/**
 *
 * @author Louis
 */
@PluginImplementation
public class CouchDBPluginSet extends PluginBase {

    private CouchDBIndexer plugIndexer;
    private CouchDBQuery plugQuery;
    private CouchDBStorage plugStorage;
    protected static StdCouchDbInstance server = null;
    private static String hostKey = "DefaultServerHost";
    private static String portKey = "DefaultServerPort";

    public CouchDBPluginSet() {
        System.out.println("INIT-->CouchDB plugin");
        plugIndexer = new CouchDBIndexer();
        this.indexPlugins.add(plugIndexer);
        plugQuery = new CouchDBQuery();
        this.queryPlugins.add(plugQuery);
        plugStorage = new CouchDBStorage();
        this.storagePlugins.add(plugStorage);
    }

    @Override
    public void setSettings(ConfigurationHolder stgs) {
        this.settings = stgs;
        String host = this.settings.getConfiguration().getString(hostKey);
        int port = this.settings.getConfiguration().getInt(portKey);
        HttpClient httpClient = null;
        try {
            httpClient = new StdHttpClient.Builder().url("http://" + host + ":" + port).build();
        } catch (MalformedURLException ex) {
            Logger.getLogger(CouchDBPluginSet.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (server == null && httpClient != null) {
            server = new StdCouchDbInstance(httpClient);
        }
        for (QueryInterface plugin : this.getQueryPlugins()) {
            plugin.setSettings(settings);
            plugin.enable();
        }
        for (IndexerInterface plugin : this.getIndexPlugins()) {
            plugin.setSettings(settings);
            plugin.enable();
        }
        for (StorageInterface plugin : this.getStoragePlugins()) {
            plugin.setSettings(settings);
            plugin.enable();
        }
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

    @Override
    public String getName() {
        return "couchdbplugin";
    }
}
