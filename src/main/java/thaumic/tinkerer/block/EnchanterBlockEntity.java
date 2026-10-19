package thaumic.tinkerer.block;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.enchantment.EnchanterCatalog;
import thaumic.tinkerer.menu.EnchanterMenu;

/** Release-164 state machine, including the raw compound cost and ignored debit result. */
public final class EnchanterBlockEntity extends BlockEntity implements WorldlyContainer,MenuProvider,thaumcraft.api.world.IMovableBlockEntity {
    public static final List<Aspect> PRIMALS=List.of(Aspects.AIR,Aspects.EARTH,Aspects.FIRE,Aspects.WATER,Aspects.ORDER,Aspects.ENTROPY);
    private final NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private final List<ResourceLocation> enchantments=new ArrayList<>();
    private final List<Integer> levels=new ArrayList<>();
    private final List<BlockPos> pillars=new ArrayList<>();
    public AspectList totalAspects=new AspectList(),currentAspects=new AspectList();
    public boolean working;
    public EnchanterBlockEntity(BlockPos pos,BlockState state){super(EnchanterContent.TILE.get(),pos,state);}
    public List<ResourceLocation> enchantments(){return List.copyOf(enchantments);}
    public List<Integer> levels(){return List.copyOf(levels);}
    public List<BlockPos> pillars(){return List.copyOf(pillars);}
    public static boolean acceptsWand(ItemStack stack){var kind=WandApi.kind(stack);if(kind==WandApi.Kind.NONE||kind==WandApi.Kind.STAFF)return false;var cap=WandCap.caps.get(WandApi.parts(stack).cap());return cap!=null&&cap.getBaseCostModifier()<=1;}
    public boolean select(Player player,ResourceLocation id,int requestedLevel){
        if(working||level==null||level.isClientSide()||!stillValid(player))return false;
        var entry=EnchanterCatalog.find(id);if(entry==null)return false;int index=enchantments.indexOf(id);
        if(requestedLevel==-1){if(index<0)return false;enchantments.remove(index);levels.remove(index);}
        else if(index<0){if(!entry.canApply(getItem(0),level.registryAccess(),enchantments)||!entry.unlocked(player))return false;enchantments.add(id);levels.add(1);}
        else levels.set(index,Math.max(1,Math.min(entry.maxLevel(),requestedLevel)));
        updateAspectList();sync();return true;
    }
    public boolean start(){if(working||enchantments.isEmpty()||levels.isEmpty())return false;working=true;sync();return true;}
    public void updateAspectList(){totalAspects=new AspectList();for(int i=0;i<enchantments.size();i++){var cost=Objects.requireNonNull(EnchanterCatalog.find(enchantments.get(i))).cost(levels.get(i));for(var aspect:cost.getAspectsSorted())totalAspects.add(aspect,cost.getAmount(aspect));}}
    /** Literal original upper limit, 2..12 plain totems followed immediately by nitor. */
    public int findPillar(int x,int y,int z){
        int count=0;for(int at=y;at<256;at++){var p=new BlockPos(x,at,z);if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p))return -1;
            var id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString();
            if(!id.equals("thaumcraft:obsidian_totem"))return id.equals("thaumcraft:nitor")&&count>=2&&count<13?at:-1;count++;
        }return -1;
    }
    private boolean assignPillars(){pillars.clear();for(int x=worldPosition.getX()-4;x<=worldPosition.getX()+4;x++)for(int z=worldPosition.getZ()-4;z<=worldPosition.getZ()+4;z++){
        int top=findPillar(x,worldPosition.getY(),z);if(top!=-1)pillars.add(new BlockPos(x,top,z));if(pillars.size()==6)return true;
    }pillars.clear();return false;}
    public boolean checkPillars(){
        if(pillars.isEmpty()){if(assignPillars())return true;working=false;currentAspects=new AspectList();return false;}
        for(int i=0;i<pillars.size();i++){var p=pillars.get(i);int top=findPillar(p.getX(),worldPosition.getY(),p.getZ());if(top==-1){pillars.clear();return checkPillars();}if(top!=p.getY())pillars.set(i,new BlockPos(p.getX(),top,p.getZ()));}return true;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,EnchanterBlockEntity tile){tile.tick();}
    private void tick(){
        if(getItem(0).isEmpty()){enchantments.clear();levels.clear();}
        if(!working)return;if(getItem(0).isEmpty()){working=false;super.setChanged();return;}
        if(!checkPillars()){super.setChanged();return;}
        for(var primal:PRIMALS)if(currentAspects.getAmount(primal)<totalAspects.getAmount(primal)){
            var wand=getItem(1);var kind=WandApi.kind(wand);
            if(kind!=WandApi.Kind.NONE&&kind!=WandApi.Kind.STAFF){
                var eligible=new ArrayList<Aspect>();for(var aspect:PRIMALS)if(totalAspects.getAmount(aspect)>currentAspects.getAmount(aspect)&&WandApi.stored(wand,aspect)>=100)eligible.add(aspect);
                int index=eligible.isEmpty()?0:level.random.nextInt(eligible.size());if(!eligible.isEmpty()){
                    var chosen=eligible.get(index);WandApi.consumeCrafting(level,wand,null,Map.of(chosen,1),!level.isClientSide());currentAspects.add(chosen,1);var pillar=pillars.get(index);
                    if(level.random.nextBoolean()){
                        ParticleApi.blockRunes(level,pillar.getX(),pillar.getY()-.75,pillar.getZ(),.3f+level.random.nextFloat()*.7f,0,.3f+level.random.nextFloat()*.7f,15,level.random.nextFloat()/8);
                        ParticleApi.blockRunes(level,worldPosition.getX(),worldPosition.getY()+.25,worldPosition.getZ(),.3f+level.random.nextFloat()*.7f,0,.3f+level.random.nextFloat()*.7f,15,level.random.nextFloat()/8);
                        if(level.random.nextInt(5)==0)sound(pillar.getX(),worldPosition.getY(),pillar.getZ(),"brain",.5f);
                    }super.setChanged();
                }
            }return;
        }
        working=false;currentAspects=new AspectList();totalAspects=new AspectList();
        if(!level.isClientSide())for(int i=0;i<enchantments.size();i++)getItem(0).enchant(Objects.requireNonNull(EnchanterCatalog.find(enchantments.get(i))).holder(level.registryAccess()),levels.get(i));
        enchantments.clear();levels.clear();sound(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),"wand",1);sync();
    }
    private void sound(double x,double y,double z,String name,float volume){level.playSound(null,x,y,z,BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",name)),SoundSource.BLOCKS,volume,1);}
    public void sync(){super.setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide()&&!working){enchantments.clear();levels.clear();sync();}}
    @Override public int getContainerSize(){return 2;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return false;}
    @Override public int[] getSlotsForFace(Direction side){return new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return false;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items.replaceAll(s->ItemStack.EMPTY);setChanged();}
    @Override public Component getDisplayName(){return Component.translatable("block.thaumictinkerer.enchanter");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new EnchanterMenu(id,inventory,this);}
    private static CompoundTag aspects(AspectList list){var out=new CompoundTag();for(var aspect:list.getAspects())out.putInt(aspect.getTag(),list.getAmount(aspect));return out;}
    private static AspectList aspects(CompoundTag tag){var out=new AspectList();for(var key:tag.getAllKeys()){var aspect=Aspect.getAspect(key);if(aspect!=null)out.add(aspect,tag.getInt(key));}return out;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);tag.putBoolean("working",working);tag.put("currentAspects",aspects(currentAspects));tag.put("totalAspects",aspects(totalAspects));var names=new ListTag();enchantments.forEach(id->names.add(StringTag.valueOf(id.toString())));tag.put("enchantments",names);tag.putIntArray("levelsIntArray",levels);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items.replaceAll(s->ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);working=tag.getBoolean("working");currentAspects=aspects(tag.getCompound("currentAspects"));totalAspects=aspects(tag.getCompound("totalAspects"));enchantments.clear();levels.clear();var names=tag.getList("enchantments",8);var saved=tag.getIntArray("levelsIntArray");for(int i=0;i<Math.min(names.size(),saved.length);i++){var id=ResourceLocation.tryParse(names.getString(i));if(id!=null&&EnchanterCatalog.find(id)!=null){enchantments.add(id);levels.add(saved[i]);}}pillars.clear();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
