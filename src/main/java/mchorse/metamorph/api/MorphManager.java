package mchorse.metamorph.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mchorse.metamorph.api.abilities.IAbility;
import mchorse.metamorph.api.abilities.IAction;
import mchorse.metamorph.api.abilities.IAttackAbility;
import mchorse.metamorph.api.models.ModelManager;
import mchorse.metamorph.api.morphs.AbstractMorph;
import mchorse.metamorph.api.morphs.EntityMorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Morph manager class
 * 
 * This manager is responsible for managing available morphings.
 */
public class MorphManager
{
    /**
     * Default <s>football</s> morph manager 
     */
    public static final MorphManager INSTANCE = new MorphManager();

    /**
     * Registered abilities 
     */
    public Map<String, IAbility> abilities = new HashMap<String, IAbility>();

    /**
     * Registered actions 
     */
    public Map<String, IAction> actions = new HashMap<String, IAction>();

    /**
     * Registered attacks 
     */
    public Map<String, IAttackAbility> attacks = new HashMap<String, IAttackAbility>();

    /**
     * Registered morph factories
     */
    public List<IMorphFactory> factories = new ArrayList<IMorphFactory>();

    /**
     * Settings for morphs for this server, this is not used for populating 
     * morph abilities and properties. See active morph settings.
     */
    public Map<String, MorphSettings> settings = new HashMap<String, MorphSettings>();

    /**
     * Active morph settings 
     */
    public Map<String, MorphSettings> activeSettings = new HashMap<String, MorphSettings>();

    /**
     * Blacklisted entities for this server, not used for actual checking, 
     * only for populating.
     */
    public Set<String> blacklist = new TreeSet<String>();

    /**
     * Active blacklist. Sent either from server, or getting assigned on 
     * server start. Don't modify this, please. 
     */
    public Set<String> activeBlacklist = new TreeSet<String>();

    /**
     * Model manager
     */
    public ModelManager models;

    /**
     * Check whether morph by the given name is blacklisted 
     */
    public static boolean isBlacklisted(String name)
    {
        return INSTANCE.activeBlacklist.contains(name);
    }

    /**
     * Set currently blacklist for usage 
     */
    public void setActiveBlacklist(Set<String> blacklist)
    {
        INSTANCE.activeBlacklist.clear();
        INSTANCE.activeBlacklist.addAll(blacklist);
    }

    /**
     * Set currently blacklist for usage 
     */
    public void setActiveSettings(Map<String, MorphSettings> settings)
    {
        INSTANCE.activeSettings.clear();
        INSTANCE.activeSettings.putAll(settings);
    }

    /**
     * That's a singleton, boy! 
     */
    private MorphManager()
    {}

    /**
     * Register all morph factories 
     */
    public void register()
    {
        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            this.factories.get(i).register(this);
        }
    }

    /**
     * Register all morph factories on the client side 
     */
    @SideOnly(Side.CLIENT)
    public void registerClient()
    {
        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            this.factories.get(i).registerClient(this);
        }
    }

    /**
     * Checks if manager has given morph by ID and NBT tag compound
     * 
     * This meethod iterates over all {@link IMorphFactory}s and if any of them 
     * returns true, then there's a morph, otherwise false.
     */
    public boolean hasMorph(String name)
    {
        if (isBlacklisted(name))
        {
            return false;
        }

        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            if (this.factories.get(i).hasMorph(name))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Get an abstract morph from NBT
     * 
     * This method iterates over all {@link IMorphFactory}s, returns a morph 
     * from the first morph factory that does have a morph.
     */
    public AbstractMorph morphFromNBT(NBTTagCompound tag)
    {
        String name = tag.getString("Name");

        if (isBlacklisted(name))
        {
            return null;
        }

        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            if (this.factories.get(i).hasMorph(name))
            {
                AbstractMorph morph = this.factories.get(i).getMorphFromNBT(tag);

                this.applySettings(morph);

                return morph;
            }
        }

        return null;
    }

    /**
     * Apply morph settings on a given morph 
     */
    public void applySettings(AbstractMorph morph)
    {
        if (this.activeSettings.containsKey(morph.name))
        {
            this.activeSettings.get(morph.name).apply(morph);
        }
    }

    /**
     * Get all morphs that factories provide. Take in account that this code 
     * don't apply morph settings.
     */
    public MorphList getMorphs(World world)
    {
        MorphList morphs = new MorphList();

        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            this.factories.get(i).getMorphs(morphs, world);
        }

        return morphs;
    }

    /**
     * Get morph from the entity
     * 
     * Here I should add some kind of mechanism that allows people to substitute 
     * the name of the morph based on the given entity (in the future with 
     * introduction of the public API).
     */
    public String morphNameFromEntity(Entity entity)
    {
        return EntityList.getEntityString(entity);
    }

    /**
     * Get display name for morph (only client)
     */
    @SideOnly(Side.CLIENT)
    public String morphDisplayNameFromMorph(AbstractMorph morph)
    {
        for (int i = this.factories.size() - 1; i >= 0; i--)
        {
            String name = this.factories.get(i).displayNameForMorph(morph);

            if (name != null)
            {
                return name;
            }
        }

        /* Falling back to default method */
        String name = morph.name;

        if (morph instanceof EntityMorph)
        {
            name = EntityList.getEntityString(((EntityMorph) morph).getEntity(Minecraft.getMinecraft().theWorld));
        }

        String key = "entity." + name + ".name";
        String result = I18n.format(key);

        return key.equals(result) ? name : result;
    }
}