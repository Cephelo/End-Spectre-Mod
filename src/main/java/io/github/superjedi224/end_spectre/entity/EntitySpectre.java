package io.github.superjedi224.end_spectre.entity;

import javax.annotation.Nullable;

import io.github.superjedi224.end_spectre.EnderMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.github.superjedi224.end_spectre.util.EndSpectreConfig;
import io.github.superjedi224.end_spectre.util.EndSpectreSounds;

public class EntitySpectre extends EntityMob
{
    /** Random offset used in floating behaviour */
    private float heightOffset = 0.5F;
    /** ticks until heightOffset is randomized */
    private int heightOffsetUpdateTime;
    private static final DataParameter<Byte> ON_FIRE = EntityDataManager.<Byte>createKey(EntitySpectre.class, DataSerializers.BYTE);

    public EntitySpectre(World worldIn)
    {
        super(worldIn);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.LAVA, 1.0F);
        this.setPathPriority(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathPriority(PathNodeType.DAMAGE_FIRE, 0.0F);
        this.isImmuneToFire = EndSpectreConfig.stats.isImmuneToFire;
        this.experienceValue = EndSpectreConfig.stats.experienceValue;
    }

    public static void registerFixesBlaze(DataFixer fixer)
    {
        EntityLiving.registerFixesMob(fixer, EntitySpectre.class);
    }

    protected void initEntityAI()
    {
        this.tasks.addTask(4, new EntitySpectre.AIFireballAttack(this));
        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 1.0D, 0.0F));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, new Class[0]));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(EndSpectreConfig.stats.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(EndSpectreConfig.stats.movementSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(EndSpectreConfig.stats.followRange);
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(ON_FIRE, Byte.valueOf((byte)0));
    }

    protected SoundEvent getAmbientSound()
    {
        return EndSpectreSounds.ENTITY_SPECTRE_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return EndSpectreSounds.ENTITY_SPECTRE_HURT;
    }

    protected SoundEvent getDeathSound()
    {
        return EndSpectreSounds.ENTITY_SPECTRE_DEATH;
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender()
    {
        return 15728880;
    }

    /**
     * Gets how bright this entity is.
     */
    public float getBrightness()
    {
        return 1.0F;
    }

    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    public void onLivingUpdate()
    {
        if (!this.onGround && this.motionY < 0.0D)
        {
            this.motionY *= 0.6D;
        }

        if (this.world.isRemote)
        {
            if (this.rand.nextInt(24) == 0 && !this.isSilent())
            {
                this.world.playSound(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D, EndSpectreSounds.ENTITY_SPECTRE_BURN, this.getSoundCategory(), 0.75F + 0.5F*this.rand.nextFloat(), this.rand.nextFloat() * 0.5F + 0.5F, false);
            }

            for (int i = 0; i < 2; ++i)
            {
                this.world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, 0.0D, 0.0D, 0.0D);
            }
        }

        super.onLivingUpdate();
    }

    protected void updateAITasks()
    {

        --this.heightOffsetUpdateTime;

        if (this.heightOffsetUpdateTime <= 0)
        {
            this.heightOffsetUpdateTime = 100;
            this.heightOffset = 0.5F + (float)this.rand.nextGaussian() * 3.0F;
        }

        EntityLivingBase entitylivingbase = this.getAttackTarget();

        if (entitylivingbase != null && entitylivingbase.posY + (double)entitylivingbase.getEyeHeight() > this.posY + (double)this.getEyeHeight() + (double)this.heightOffset)
        {
            this.motionY += (0.30000001192092896D - this.motionY) * 0.30000001192092896D;
            this.isAirBorne = true;
        }

        super.updateAITasks();
    }

    public void fall(float distance, float damageMultiplier)
    {
    }

    /**
     * Returns true if the entity is on fire. Used by render to add the fire effect on rendering.
     */
    public boolean isBurning()
    {
        return this.isCharged();
    }

    @Nullable
    protected ResourceLocation getLootTable()
    {
        return new ResourceLocation(EnderMod.MODID,"entities/ender_spectre");
    }

    public boolean isCharged()
    {
        return (((Byte)this.dataManager.get(ON_FIRE)).byteValue() & 1) != 0;
    }

    public void setOnFire(boolean onFire)
    {
        byte b0 = ((Byte)this.dataManager.get(ON_FIRE)).byteValue();

        if (onFire)
        {
            b0 = (byte)(b0 | 1);
        }
        else
        {
            b0 = (byte)(b0 & -2);
        }

        this.dataManager.set(ON_FIRE, Byte.valueOf(b0));
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (this.isEntityInvulnerable(source))
        {
            return false;
        }
        else if (source instanceof EntityDamageSourceIndirect)
        {
            for (int i = 0; i < 64; ++i)
            {
                if (this.teleportRandomly())
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            boolean flag = super.attackEntityFrom(source, amount);

            if (source.isUnblockable() && this.rand.nextInt(10) != 0)
            {
                this.teleportRandomly();
            }

            return flag;
        }
    }
    
    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean flag = super.attackEntityAsMob(entityIn);

        if (flag && this.getHeldItemMainhand().isEmpty() && entityIn instanceof EntityLivingBase)
        {
            float f = this.world.getDifficultyForLocation(new BlockPos(this)).getAdditionalDifficulty();
            if(((EntityLivingBase)entityIn).attemptTeleport(entityIn.posX+1.5*rand.nextGaussian(), entityIn.posY+rand.nextInt(3), entityIn.posZ+1.5*rand.nextGaussian())){
            	entityIn.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT,1.0f,1.0f);
            };
            ((EntityLivingBase)entityIn).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100 * (int)f));
        }

        return flag;
    }

	protected boolean teleportRandomly()
    {
        double d0 = this.posX + (this.rand.nextDouble() - 0.5D) * 24.0D;
        double d1 = this.posY + (double)(this.rand.nextInt(16) - 8);
        double d2 = this.posZ + (this.rand.nextDouble() - 0.5D) * 24.0D;
        return this.teleportTo(d0, d1, d2);
    }
    private boolean teleportTo(double x, double y, double z)
    {
        net.minecraftforge.event.entity.living.EnderTeleportEvent event = new net.minecraftforge.event.entity.living.EnderTeleportEvent(this, x, y, z, 0);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return false;
        boolean flag = this.attemptTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ());

        if (flag)
        {
            this.world.playSound((EntityPlayer)null, this.prevPosX, this.prevPosY, this.prevPosZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
            this.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.0F);
        }

        return flag;
    }
    
    protected boolean isValidLightLevel()
    {
    	return Math.sqrt(this.posX*this.posX+this.posZ*this.posZ)>800;
    }

    static class AIFireballAttack extends EntityAIBase
        {
            private final EntitySpectre blaze;
            private int attackStep;
            private int attackTime;

            public AIFireballAttack(EntitySpectre blazeIn)
            {
                this.blaze = blazeIn;
                this.setMutexBits(3);
            }

            /**
             * Returns whether the EntityAIBase should begin execution.
             */
            public boolean shouldExecute()
            {
                EntityLivingBase entitylivingbase = this.blaze.getAttackTarget();
                return entitylivingbase != null && entitylivingbase.isEntityAlive();
            }

            /**
             * Execute a one shot task or start executing a continuous task
             */
            public void startExecuting()
            {
                this.attackStep = 0;
            }

            /**
             * Reset the task's internal state. Called when this task is interrupted by another one
             */
            public void resetTask()
            {
                this.blaze.setOnFire(false);
            }

            /**
             * Keep ticking a continuous task that has already been started
             */
            public void updateTask()
            {
                --this.attackTime;
                EntityLivingBase entitylivingbase = this.blaze.getAttackTarget();
                double d0 = this.blaze.getDistanceSq(entitylivingbase);

                if (d0 < 4.0D)
                {
                    if (this.attackTime <= 0)
                    {
                        this.attackTime = 20;
                        this.blaze.attackEntityAsMob(entitylivingbase);
                    }

                    this.blaze.getMoveHelper().setMoveTo(entitylivingbase.posX, entitylivingbase.posY, entitylivingbase.posZ, 1.0D);
                }
                else if (d0 < this.getFollowDistance() * this.getFollowDistance())
                {
                    double d1 = entitylivingbase.posX - this.blaze.posX;
                    double d2 = entitylivingbase.getEntityBoundingBox().minY + (double)(entitylivingbase.height / 2.0F) - (this.blaze.posY + (double)(this.blaze.height / 2.0F));
                    double d3 = entitylivingbase.posZ - this.blaze.posZ;

                    if (this.attackTime <= 0)
                    {
                        ++this.attackStep;

                        if (this.attackStep == 1)
                        {
                            this.attackTime = 50;
                            //this.blaze.setOnFire(true);
                        }
                        else if (this.attackStep <= 4)
                        {
                            this.attackTime = 6;
                        }
                        else
                        {
                            this.attackTime = 85;
                            this.attackStep = 0;
                            this.blaze.setOnFire(false);
                        }

                        if (this.attackStep > 1)
                        {
                            float f = MathHelper.sqrt(MathHelper.sqrt(d0)) * 0.5F;
                            this.blaze.playSound(EndSpectreSounds.ENTITY_SPECTRE_SHOOT, 1.0f, 1.0f);

                            for (int i = 0; i < 1; ++i)
                            {
                                EntitySpectreFireball entitysmallfireball = new EntitySpectreFireball(this.blaze.world, this.blaze, 1.3f*(d1 + this.blaze.getRNG().nextGaussian() * (double)f), 1.3f*d2, 1.3f*(d3 + this.blaze.getRNG().nextGaussian() * (double)f));
                                entitysmallfireball.posY = this.blaze.posY + (double)(this.blaze.height / 2.0F) + 0.5D;
                                this.blaze.world.spawnEntity(entitysmallfireball);
                            }
                        }
                    }

                    this.blaze.getLookHelper().setLookPositionWithEntity(entitylivingbase, 10.0F, 10.0F);
                }
                else
                {
                    this.blaze.getNavigator().clearPath();
                    this.blaze.getMoveHelper().setMoveTo(entitylivingbase.posX, entitylivingbase.posY, entitylivingbase.posZ, 1.0D);
                }

                super.updateTask();
            }

            private double getFollowDistance()
            {
                IAttributeInstance iattributeinstance = this.blaze.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
                return iattributeinstance == null ? 16.0D : iattributeinstance.getAttributeValue();
            }
        }
}
