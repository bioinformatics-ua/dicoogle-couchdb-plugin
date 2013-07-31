/*  Copyright   2010 - IEETA
 *
 *  This file is part of Dicoogle.
 *
 *  Author: Luís A. Bastião Silva <bastiao@ua.pt>
 *
 *  Dicoogle is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Dicoogle is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.dicoogle.couchdbplugin;


import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dcm4che2.data.Tag;



/**
 *
 * @author Luís A. Bastião Silva <bastiao@ua.pt>
 */
public class Dictionary
{

    private Hashtable<String, Integer> tagList = new Hashtable<String, Integer>();
    private Hashtable<Integer, String> tagListByTag = new Hashtable<Integer, String>();


    private static final Dictionary instance =  new Dictionary(); ;

    public static Dictionary getInstance()
    {
        return instance;
    }

    private Dictionary()
    {

        Field [] tags = Tag.class.getFields();
        for (int i = 0 ; i<tags.length; i++)
        {
            try {
                tagList.put(tags[i].getName(), tags[i].getInt(null));
                tagListByTag.put(tags[i].getInt(null),tags[i].getName());
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Dictionary.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Dictionary.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public String tagName(int tag)
    {
        return this.tagListByTag.get(tag);
    }

    public String toString()
    {
        String str = "" ;
        Iterator<String> it = (Iterator<String>) getTagList().keySet().iterator();
        while(it.hasNext())
        {
            String key = it.next();
            str+="Name: " + key + " with value: " +
                    Integer.toHexString( this.getTagList().get(key));
        }
        return str ;
    }


    /*public static void  main(String args[]) throws IllegalArgumentException, IllegalAccessException
    {
        Dictionary da = new Dictionary();
        
    }*/

    /**
     * @return the tagList
     */
    public Hashtable<String, Integer> getTagList() {
        return tagList;
    }

    /**
     * @param tagList the tagList to set
     */
    public void setTagList(Hashtable<String, Integer> tagList) {
        this.tagList = tagList;
    }

}
