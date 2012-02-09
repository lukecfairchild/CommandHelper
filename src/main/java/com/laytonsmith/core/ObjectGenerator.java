/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.core;

import com.laytonsmith.abstraction.MCEnchantment;
import com.laytonsmith.abstraction.MCItemStack;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCWorld;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.ItemGlassBottle;

/**
 * This file is responsible for converting CH objects into server objects,
 * and vice versa
 * @author layton
 */
public class ObjectGenerator {
    
    private static ObjectGenerator pog = null;
    public static ObjectGenerator GetGenerator(){
        if(pog == null){
            pog = new ObjectGenerator();
        }
        return pog;
    }
    
    /**
     * Gets a Location Object, given a MCLocation
     * @param l
     * @return 
     */
    public CArray location(MCLocation l){
        CArray ca = new CArray(0, null);
        Construct x = new CDouble(l.getX(), 0, null);
        Construct y = new CDouble(l.getY(), 0, null);
        Construct z = new CDouble(l.getZ(), 0, null);
        Construct world = new CString(l.getWorld().getName(), 0, null);
        Construct yaw = new CDouble(l.getYaw(), 0, null);
        Construct pitch = new CDouble(l.getPitch(), 0, null);
        ca.forceAssociativeMode();
        ca.set("0", x);
        ca.set("1", y);
        ca.set("2", z);
        ca.set("3", world);
        ca.set("4", yaw);
        ca.set("5", pitch);
        ca.set("x", x);
        ca.set("y", y);
        ca.set("z", z);
        ca.set("world", world);
        ca.set("yaw", yaw);
        ca.set("pitch", pitch);
        return ca;
    }
    
    /**
     * Given a Location Object, returns a MCLocation. If the optional world is
     * not specified in the object, the world provided is used instead.
     * Location "objects" are mscript arrays that represent a location in game. There are 
     * 4 usages:
     * <ul>
     * <li>(x, y, z)</li>
     * <li>(x, y, z, world)</li>
     * <li>(x, y, z, yaw, pitch)</li>
     * <li>(x, y, z, world, yaw, pitch)</li>
     * </ul>
     * In all cases, the pitch and yaw default to 0, and the world defaults to the specified world.
     * <em>More conveniently: ([world], x, y, z, [yaw, pitch])</em> 
     */    
    public MCLocation location(Construct c, MCWorld w, int line_num, File f) {
        if (!(c instanceof CArray)) {
            throw new ConfigRuntimeException("Expecting an array, received " + c.getCType(), ExceptionType.FormatException, line_num, f);
        }
        CArray array = (CArray) c;
        MCWorld world = w;
        double x = 0;
        double y = 0;
        double z = 0;
        float yaw = 0;
        float pitch = 0;
        if (array.size() == 3) {
            //Just the xyz, with default yaw and pitch, and given world
            x = Static.getNumber(array.get(0, line_num, f));
            y = Static.getNumber(array.get(1, line_num, f));
            z = Static.getNumber(array.get(2, line_num, f));
        } else if (array.size() == 4) {
            //x, y, z, world
            x = Static.getNumber(array.get(0, line_num, f));
            y = Static.getNumber(array.get(1, line_num, f));
            z = Static.getNumber(array.get(2, line_num, f));
            world = Static.getServer().getWorld(array.get(3, line_num, f).val());
        } else if (array.size() == 5) {
            //x, y, z, yaw, pitch, with given world
            x = Static.getNumber(array.get(0, line_num, f));
            y = Static.getNumber(array.get(1, line_num, f));
            z = Static.getNumber(array.get(2, line_num, f));
            yaw = (float) Static.getNumber(array.get(3, line_num, f));
            pitch = (float) Static.getNumber(array.get(4, line_num, f));
        } else if (array.size() == 6) {
            //All have been given
            x = Static.getNumber(array.get(0, line_num, f));
            y = Static.getNumber(array.get(1, line_num, f));
            z = Static.getNumber(array.get(2, line_num, f));
            world = Static.getServer().getWorld(array.get(3, line_num, f).val());
            yaw = (float) Static.getNumber(array.get(4, line_num, f));
            pitch = (float) Static.getNumber(array.get(5, line_num, f));
        } else {
            throw new ConfigRuntimeException("Expecting a Location array, but the array did not meet the format specifications", ExceptionType.FormatException, line_num, f);
        }
        if(array.containsKey("x")){
            x = Static.getNumber(array.get("x"));
        }
        if(array.containsKey("y")){
            y = Static.getNumber(array.get("y"));
        }
        if(array.containsKey("z")){
            z = Static.getNumber(array.get("z"));
        }
        if(array.containsKey("world")){
            world = Static.getServer().getWorld(array.get("world").val());
        }
        if(array.containsKey("yaw")){
            yaw = (float)Static.getDouble(array.get("yaw"));
        }
        if(array.containsKey("pitch")){
            pitch = (float)Static.getDouble(array.get("pitch"));
        }
        return StaticLayer.GetLocation(world, x, y, z, yaw, pitch);
    }
    
    /**
     * An Item Object consists of data about a particular item stack.
     * Information included is: type, data, qty, and an array of
     * enchantment objects (labeled enchants): etype (enchantment type) and 
     * elevel (enchantment level). For backwards compatibility, this information
     * is also listed in numerical slots as well as associative slots. If the MCItemStack
     * is null, or the underlying item is nonexistant (or air) CNull is returned.     
     * @param is
     * @return 
     */
    public Construct item(MCItemStack is, int line_num, File f){
        if(is == null || is.getAmount() == 0){
            return new CNull(line_num, f);
        }
        int type = is.getTypeId();
        int data = (is.getData()!=null?is.getData().getData():0);
        int qty = is.getAmount();
        CArray enchants = new CArray(line_num, f);
        for(Map.Entry<MCEnchantment, Integer> entry : is.getEnchantments().entrySet()){
            CArray enchObj = new CArray(line_num, f);
            enchObj.forceAssociativeMode();
            enchObj.set(0, new CString(entry.getKey().getName(), line_num, f));
            enchObj.set("etype", new CString(entry.getKey().getName(), line_num, f));
            enchObj.set(1, new CInt(entry.getValue(), line_num, f));
            enchObj.set("elevel", new CInt(entry.getValue(), line_num, f));
            enchants.push(enchObj);
        }
        CArray ret = new CArray(line_num, f);
        ret.forceAssociativeMode();
        ret.set("0", type + (data!=0?(":" + data):""));
        ret.set("type", Integer.toString(type));
        ret.set("data", Integer.toString(data));
        ret.set("1", Integer.toString(qty));
        ret.set("qty", Integer.toString(qty));
        ret.set("2", enchants);
        ret.set("enchants", enchants);
        return ret;
    }
    
    /**
     * Gets an MCItemStack from a given item "object". Supports both the old
     * and new formats currently
     * @param i
     * @param line_num
     * @param f
     * @return 
     */
    public MCItemStack item(Construct i, int line_num, File f){
        if(!(i instanceof CArray)){
            throw new ConfigRuntimeException("Expected an array!", ExceptionType.FormatException, line_num, f);
        }
        //We need to support
        //both the new version, and the old version, but the new version
        //supersedes the old version. Also, either version
        //might be present, because the old version should be phased out eventually,
        //so let's also give a deprecation warning
        //First, try to pull out the old items
        CArray item = (CArray)i;
        int type = 0;
        int data = 0;
        int qty = 0;
        Map<MCEnchantment, Integer> enchants = new HashMap<MCEnchantment, Integer>();
        boolean depWarning = false;
        if(item.containsKey(0) || item.containsKey("type") || item.containsKey("data")){
            //This is the type and possibly data segment
            String typeData = "Expecting a number";
            if(item.containsKey(0)){
                typeData = item.get(0).val();
                depWarning = true;
            }            
            if(item.containsKey("type")){
                String td = item.get("type").val();
                if(item.containsKey("data") && !item.get("data").val().equals("0")){
                    td += ":" + item.get("data");
                }
                typeData = td;
            }
            String[] split = typeData.split(":");
            try{
                type = Integer.parseInt(split[0]);
            }catch(NumberFormatException e){
                throw new ConfigRuntimeException("Could not get item information from given information (" + split[0] + ")", ExceptionType.FormatException, line_num, f, e);
            }
            if(split.length == 2){
                try{
                    data = Integer.parseInt(split[1]);
                } catch(NumberFormatException e){
                    throw new ConfigRuntimeException("Could not get item data from given information (" + split[1] + ")", ExceptionType.FormatException, line_num, f, e);                    
                }
            }
        } else {
            throw new ConfigRuntimeException("Could not find item data!", ExceptionType.FormatException, line_num, f);
        }
        if(item.containsKey(1) || item.containsKey("qty")){
            //This is the qty
            String sqty = "notanumber";
            if(item.containsKey(1)){
                sqty = item.get(1).val();
                depWarning = true;
            }
            if(item.containsKey("qty")){
                sqty = item.get("qty").val();
            }
            try{
                qty = Integer.parseInt(sqty);
            } catch(NumberFormatException e){
                throw new ConfigRuntimeException("Could not get qty from given information (" + sqty + ")", ExceptionType.FormatException, line_num, f, e);                                    
            }
        }
        
        if(item.containsKey(2) || item.containsKey("enchants")){
            CArray enchantArray = null;
            try{
                if(item.containsKey(2)){
                    enchantArray = (CArray)item.get(2);
                    depWarning = true;
                }
                if(item.containsKey("enchants")){
                    enchantArray = (CArray)item.get("enchants");
                }
                if(enchantArray == null){
                    throw new NullPointerException();
                }
            } catch(Exception e){
                throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, line_num, f, e);
            }
            
            for(String index : enchantArray.keySet()){
                try{
                    CArray enchantment = (CArray)enchantArray.get(index);
                    String setype = null;
                    String selevel = null;
                    if(enchantment.containsKey(0) || enchantment.containsKey("etype")){
                        if(enchantment.containsKey(0)){
                            setype = enchantment.get(0).val();
                            depWarning = true;
                        }
                        if(enchantment.containsKey("etype")){
                            setype = enchantment.get("etype").val();
                        }
                    }
                    
                    if(enchantment.containsKey(1) || enchantment.containsKey("elevel")){
                        if(enchantment.containsKey(1)){
                            selevel = enchantment.get(1).val();
                            depWarning = true;
                        }
                        if(enchantment.containsKey("elevel")){
                            selevel = enchantment.get("elevel").val();
                        }                        
                    }
                    if(setype == null || selevel == null){
                        throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, line_num, f);                                            
                    }
                    int elevel = 0;
                    try{
                        elevel = Integer.parseInt(selevel);
                    } catch(NumberFormatException e){                        
                        throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, line_num, f);                                            
                    }
                    MCEnchantment etype = StaticLayer.GetEnchantmentByName(setype);
                    enchants.put(etype, elevel);
                } catch(ClassCastException e){
                    throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, line_num, f, e);                    
                }
            }
        }
        MCItemStack ret = StaticLayer.GetItemStack(type, qty);
        ret.setData(data);
        for(Map.Entry<MCEnchantment, Integer> entry : enchants.entrySet()){
            ret.addEnchantment(entry.getKey(), entry.getValue());
        }
        if(depWarning){
            ConfigRuntimeException.DoWarning("Using old item objects; this may not be supported in the future!");
        }
        return ret;        
    }
}
