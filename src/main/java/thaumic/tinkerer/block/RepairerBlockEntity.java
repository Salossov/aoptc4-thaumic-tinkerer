package thaumic.tinkerer.block;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.api.client.ParticleApi;

/** Release-164 repairer. Suction TYPE of the source, rather than its stored type, is intentional. */
public final class RepairerBlockEntity extends BlockEntity implements WorldlyContainer,IAspectContainer,IEssentiaTransport,thaumcraft.api.world.IMovableBlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public int ticksExisted;
    public boolean tookLastTick=true;
    private int dmgLastTick;
    public RepairerBlockEntity(BlockPos pos,BlockState state){super(RepairerContent.TILE.get(),pos,state);}
    public Direction orientation(){return getBlockState().getValue(RepairerBlock.FACING);}
    public static boolean accepts(ItemStack stack){return !stack.isEmpty()&&stack.getItem().isRepairable(stack);}
    public static void tick(Level level,BlockPos pos,BlockState state,RepairerBlockEntity tile){
        if(++tile.ticksExisted%10!=0)return;
        var stack=tile.getItem(0);int before=stack.isEmpty()?0:stack.getDamageValue();
        if(before>0){
            if(!level.isClientSide()&&level.getServer().isSameThread()){
                stack.setDamageValue(Math.max(0,before-tile.drawEssentia()));tile.setChanged();
            }
            if(tile.dmgLastTick!=0&&tile.dmgLastTick!=before){
                ParticleApi.sparkle(level,(float)(pos.getX()+.25+Math.random()/2),(float)(pos.getY()+1+Math.random()/2),(float)(pos.getZ()+.25+Math.random()/2),0);
                tile.tookLastTick=true;
            }else tile.tookLastTick=false;
        }else tile.tookLastTick=false;
        tile.dmgLastTick=stack.isEmpty()?0:stack.getDamageValue();
    }
    private int drawEssentia(){
        var face=orientation();var target=worldPosition.relative(face);
        if(!level.hasChunkAt(target))return 0;
        var source=level.getCapability(EssentiaCapabilities.TRANSPORT,target,face.getOpposite());
        if(source==null||!source.isConnectable(face.getOpposite())||!source.canOutputTo(face.getOpposite()))return 0;
        var type=source.getSuctionType(face.getOpposite());int repair=type==Aspects.TOOL?8:type==Aspects.CRAFT?5:type==Aspects.ORDER?3:0;
        return repair>0&&source.getSuctionAmount(face.getOpposite())<getSuctionAmount(face)&&source.takeEssentia(type,1,face.getOpposite())==1?repair:0;
    }
    @Override public void setChanged(){super.setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.getFirst().isEmpty();}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return true;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items.set(0,ItemStack.EMPTY);setChanged();}
    @Override public int[] getSlotsForFace(Direction side){return new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return accepts(stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return true;}
    @Override public AspectList getAspects(){return isEmpty()?null:new AspectList().add(Aspects.ENTROPY,getItem(0).getDamageValue());}
    @Override public void setAspects(AspectList aspects){}
    @Override public boolean doesContainerAccept(Aspect aspect){return false;}
    @Override public int addToContainer(Aspect aspect,int amount){return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return false;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount){return false;}
    @Override public int containerContains(Aspect aspect){return 0;}
    @Override public boolean isConnectable(Direction face){return face==orientation();}
    @Override public boolean canInputFrom(Direction face){return false;}
    @Override public boolean canOutputTo(Direction face){return isConnectable(face);}
    @Override public void setSuction(Aspect aspect,int amount){}
    @Override public Aspect getSuctionType(Direction face){return isConnectable(face)?Aspects.TOOL:null;}
    @Override public int getSuctionAmount(Direction face){return isConnectable(face)?128:0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){return 0;}
    @Override public Aspect getEssentiaType(Direction face){return null;}
    @Override public int getEssentiaAmount(Direction face){return 0;}
    @Override public int getMinimumSuction(){return 0;}
    @Override public boolean renderExtendedTube(){return false;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items.set(0,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
