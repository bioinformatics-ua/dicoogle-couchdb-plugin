/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.dcm4che2.data.Tag;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.DesignDocument;
import org.ektorp.support.SimpleViewGenerator;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 *
 * @author Louis
 */
public class CouchDBQuery implements QueryInterface {

    private boolean isEnable = false;
    private URI location;
    private ConfigurationHolder settings = null;
    private CouchDbConnector db;
    private String host;
    private String dbName;
    private int port;
    private static String hostKey = "DefaultServerHost";
    private static String portKey = "DefaultServerPort";
    private static String dbNameKey = "IndexDataBase";

    public CouchDBQuery() {
        System.out.println("Init -> CouchDB Query");
    }

    @Override
    public Iterable<SearchResult> query(String strQuery, Object... os) {
        String queryResult = CouchDBUtil.parseStringToQuery(strQuery);

        String ddID = "_design/Dicoogle";
        String ddName = "_temp";
        DesignDocument.View v = new DesignDocument.View(queryResult);
        DesignDocument dd = new DesignDocument(ddID);
        dd.addView(ddName, v);
        
        if (db.contains(ddID)) {
            DesignDocument ddToDel = db.get(DesignDocument.class, ddID);
            db.delete(ddToDel);
        }
        db.update(dd);
        
        ViewQuery vQuery = new ViewQuery().viewName(ddName).designDocId(ddID);
        ViewResult vResult = db.queryView(vQuery);
        db.delete(dd);
        
        List<SearchResult> result = CouchDBUtil.getListFromViewResult(vResult, location, (float)0.0);
        return result;
    }

    @Override
    public String getName() {
        return "couchdbplugin";
    }

    @Override
    public boolean enable() {
        if (this.settings == null || CouchDBPluginSet.server == null) {
            return false;
        }
        try {
            location = new URI("http://" + host + ":" + port + "/" + dbName + "/");
        } catch (URISyntaxException e) {
            return false;
        }
        this.isEnable = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.isEnable = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnable;
    }

    @Override
    public void setSettings(ConfigurationHolder ch) {
        this.settings = ch;
        host = settings.getConfiguration().getString(hostKey);
        port = settings.getConfiguration().getInt(portKey);
        dbName = this.settings.getConfiguration().getString(dbNameKey);
        this.db = CouchDBPluginSet.server.createConnector(dbName, true);
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }
}
