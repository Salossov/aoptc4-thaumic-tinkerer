package thaumic.tinkerer.block;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.LoggerFactory;
import thaumic.tinkerer.menu.AnimationTabletMenu;

/** Original swing/detection/mining order. A player's held slot is intentionally empty between swing hits. */
public final class AnimationTabletBlockEntity extends BlockEntity implements Container,MenuProvider,thaumcraft.api.world.IMovableBlockEntity {
    private ItemStack stack=ItemStack.EMPTY;
    public double ticksExisted;
    public boolean leftClick=true,redstone;
    public int swingProgress;
    private int swingMod,initialDamage,curblockDamage,durabilityRemainingOnBlock;
    private boolean breaking;
    private List<Entity> detected=List.of();
    private TabletPlayer player;
    private TabletPreparation preparation;
    public AnimationTabletBlockEntity(BlockPos pos,BlockState state){super(AnimationTabletContent.TILE.get(),pos,state);}
    public int rotation(){return Arrays.asList(AnimationTabletBlock.DIRECTIONS).indexOf(getBlockState().getValue(AnimationTabletBlock.FACING));}
    /** Original Lua getter includes the powered metadata bit. */
    public int reportedRotation(){return rotation()+(getBlockState().getValue(AnimationTabletBlock.POWERED)?8:0);}
    public void rotation(int value){if(value<0||value>3)throw new IllegalArgumentException("Invalid value: "+value+".");level.setBlock(worldPosition,getBlockState().setValue(AnimationTabletBlock.FACING,AnimationTabletBlock.DIRECTIONS[value]).setValue(AnimationTabletBlock.POWERED,false),3);}
    public BlockPos target(){return worldPosition.relative(getBlockState().getValue(AnimationTabletBlock.FACING));}
    public boolean isBreaking(){return breaking;}
    @Override public boolean prepareToMove(){cancelPreparation();stopBreaking();return true;}
    @Override public void setRemoved(){cancelPreparation();super.setRemoved();}
    @Override public void onChunkUnloaded(){cancelPreparation();super.onChunkUnloaded();}
    public TabletPlayer player(){if(player==null&&level instanceof ServerLevel server)player=new TabletPlayer(server,this);return player;}
    public void findEntities(){var target=target();detected=level.hasChunkAt(target)?level.getEntities((Entity)null,new AABB(target)):List.of();}
    public boolean detect(){if(level instanceof ServerLevel&&!prepared(0))return false;findEntities();return level.hasChunkAt(target())&&(!level.getBlockState(target()).isAir()||!detected.isEmpty());}
    public void initiateSwing(){swingMod=3;swingProgress=1;}
    public boolean trigger(){if(swingProgress!=0)return false;findEntities();initiateSwing();level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);return true;}
    public static void tick(Level level,BlockPos pos,BlockState state,AnimationTabletBlockEntity tile){tile.tick();}
    private void tick(){
        if(!level.isClientSide())player().prepare();ticksExisted++;
        if(preparation!=null&&!preparation.valid()){cancelPreparation();stopBreaking();return;}
        try{
        if(!stack.isEmpty()){
            if(swingProgress>=45&&!level.isClientSide()){swingHit();if(preparation!=null&&!preparation.ready())return;}
            swingMod=swingProgress<=0?0:swingProgress>=45?-3:swingMod;swingProgress+=swingMod;if(swingProgress<0)swingProgress=0;
        }else{swingMod=0;swingProgress=0;if(breaking)stopBreaking();}
        if(level instanceof ServerLevel&&!prepared(breaking?18:0))return;
        boolean detect=detect();if(!detect)stopBreaking();if(detect&&breaking&&!level.isClientSide())continueBreaking();
        if((!redstone||breaking)&&detect&&swingProgress==0){initiateSwing();if(!level.isClientSide())level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);}
        }finally{releasePrepared();}
    }
    private boolean prepared(int radius){
        if(!(level instanceof ServerLevel server)||!liveOwner(server))return false;
        var columns=TabletPreparation.columns(worldPosition,target(),radius);
        if(preparation!=null&&!preparation.valid()){cancelPreparation();stopBreaking();return false;}
        if(preparation!=null&&!preparation.covers(columns)){preparation.close();preparation=null;}
        if(preparation==null){if(TabletPreparation.loaded(server,columns))return true;preparation=new TabletPreparation(this,columns);}
        try{return preparation.advance();}catch(RuntimeException|Error failure){cancelPreparation();throw failure;}
    }
    private void cancelPreparation(){
        if(preparation!=null){preparation.close();preparation=null;swingProgress=0;swingMod=0;}
    }
    private void releasePrepared(){if(preparation!=null&&preparation.ready()){preparation.close();preparation=null;}}
    public void swingHit(){
        if(stack.isEmpty()||level.isClientSide()||!prepared(18))return;
        // Only a deferred hit refreshes the detection that was unavailable while waiting.
        if(preparation!=null)findEntities();
        try{performSwingHit();}finally{releasePrepared();}
    }
    private void performSwingHit(){
        var p=target();if(!level.hasChunkAt(p))return;var state=level.getBlockState(p);var held=stack;var fake=player();fake.setItemInHand(InteractionHand.MAIN_HAND,held);boolean done=false;
        if(leftClick){
            var entity=detected.isEmpty()?null:detected.get(level.random.nextInt(detected.size()));
            if(entity!=null){fake.weaponAttributes(held);fake.attack(entity);done=true;}
            else if(!breaking&&!state.isAir()&&state.getDestroySpeed(level,p)>=0){breaking=true;startBreaking();done=true;}
        }else{
            var face=getBlockState().getValue(AnimationTabletBlock.FACING).getOpposite();if(state.isAir()){p=p.below();face=Direction.UP;state=level.getBlockState(p);}
            try{
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(fake,InteractionHand.MAIN_HAND));
                var entity=detected.isEmpty()?null:detected.get(level.random.nextInt(detected.size()));
                done=entity instanceof Mob mob&&(held.interactLivingEntity(fake,mob,InteractionHand.MAIN_HAND).consumesAction()||!(mob instanceof Animal)||mob.interact(fake,InteractionHand.MAIN_HAND).consumesAction());
                // Original hit coordinates are local 0/0/0 even after switching the clicked face.
                var hit=new BlockHitResult(Vec3.atLowerCornerOf(p),face,p,false);var context=new UseOnContext(level,fake,InteractionHand.MAIN_HAND,held,hit);
                if(!done)held.onItemUseFirst(context); // source ignores its result
                if(!done){var used=state.useItemOn(held,level,fake,InteractionHand.MAIN_HAND,hit);done=used.result().consumesAction();if(!done)done=state.useWithoutItem(level,fake,hit).consumesAction();}
                if(!done)done=held.useOn(context).consumesAction();
                if(!done){held.getItem().use(level,fake,InteractionHand.MAIN_HAND);done=true;} // source ignores replacement return value
            }catch(Throwable exception){LoggerFactory.getLogger("ThaumicTinkerer").error("Tool Dynamism Tablet right-click failed at {}",worldPosition,exception);
                for(var recipient:level.getEntitiesOfClass(Player.class,new AABB(worldPosition.getX()-8,worldPosition.getY()-8,worldPosition.getZ()-8,worldPosition.getX()+8,worldPosition.getY()+8,worldPosition.getZ()+8))){recipient.displayClientMessage(Component.literal("Something went wrong with a Tool Dynamism Tablet! Check your FML log.").withStyle(net.minecraft.ChatFormatting.RED),false);recipient.displayClientMessage(Component.literal(String.valueOf(exception.getMessage())).withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.ITALIC),false);}
            }
        }
        if(done){var current=fake.getMainHandItem();if(current.isEmpty())setItem(0,ItemStack.EMPTY);else if(current!=stack)setItem(0,current);sync();}super.setChanged();
    }
    public void stopBreaking(){breaking=false;if(level instanceof ServerLevel server&&player!=null)server.destroyBlockProgress(player.getId(),target(),-1);}
    private void startBreaking(){
        var p=target();var fake=player();var state=level.getBlockState(p);var event=NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,getBlockState().getValue(AnimationTabletBlock.FACING).getOpposite(),PlayerInteractEvent.LeftClickBlock.Action.START));
        if(event.isCanceled()){stopBreaking();return;}initialDamage=curblockDamage;if(event.getUseBlock()!=TriState.FALSE)state.attack(level,p,fake);float hardness=state.getDestroyProgress(fake,level,p);
        if(event.getUseItem()==TriState.FALSE){stopBreaking();return;}if(hardness>=1){tryHarvestBlock();stopBreaking();}else{durabilityRemainingOnBlock=(int)(hardness*10);((ServerLevel)level).destroyBlockProgress(fake.getId(),p,durabilityRemainingOnBlock);}
    }
    private void continueBreaking(){
        curblockDamage++;var p=target();var state=level.getBlockState(p);if(state.is(Blocks.AIR)){stopBreaking();return;}float damage=state.getDestroyProgress(player(),level,p)*(curblockDamage-initialDamage);int stage=(int)(damage*10);
        if(stage!=durabilityRemainingOnBlock){((ServerLevel)level).destroyBlockProgress(player().getId(),p,stage);durabilityRemainingOnBlock=stage;}if(damage>=1){tryHarvestBlock();stopBreaking();}
    }
    public boolean tryHarvestBlock(){
        if(!(level instanceof ServerLevel server)||!server.getServer().isSameThread()||!liveOwner(server))return false;var p=target();
        if(!server.isInWorldBounds(p)||!server.getWorldBorder().isWithinBounds(p)||server.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4)==null)return false;
        var state=level.getBlockState(p);var fake=player();var tool=stack;
        if(state.isAir()||!server.mayInteract(fake,p)||fake.blockActionRestricted(server,p,fake.gameMode.getGameModeForPlayer())
                ||state.getBlock() instanceof net.minecraft.world.level.block.GameMasterBlock&&!fake.canUseGameMasterBlocks())return false;
        if(!tool.isEmpty()&&!tool.getItem().canAttackBlock(state,level,p,fake))return false;
        var before=tool.copy();var ownerState=getBlockState();boolean wasLeftClick=leftClick,wasRedstone=redstone;
        if(net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(server,fake.gameMode.getGameModeForPlayer(),fake,p,state).isCanceled())return false;
        if(!liveOwner(server)||getBlockState()!=ownerState||leftClick!=wasLeftClick||redstone!=wasRedstone||!target().equals(p)||stack!=tool||!ItemStack.matches(tool,before)||server.getBlockState(p)!=state)return false;
        // The source passes the stored tool even when its transient player's
        // held slot was cleared by the intervening tablet tick. Keep that hand.
        if(tool.getItem() instanceof thaumcraft.api.world.IBlockStartBreakTool hook&&hook.onBlockStartBreak(tool,p,fake))return false;
        if(!liveOwner(server)||stack!=tool||getBlockState()!=ownerState||!target().equals(p))return false;
        state=server.getBlockState(p);if(state.isAir())return false;
        boolean harvest=state.canHarvestBlock(level,p,fake);var tile=level.getBlockEntity(p);if(!tool.isEmpty())tool.mineBlock(level,state,p,fake);state.getBlock().playerWillDestroy(level,p,state,fake);
        boolean removed=state.onDestroyedByPlayer(level,p,fake,harvest,state.getFluidState());if(removed){state.getBlock().destroy(level,p,state);if(harvest)state.getBlock().playerDestroy(level,fake,p,state,tile,fake.getMainHandItem());}return removed;
    }
    private boolean liveOwner(ServerLevel server){
        if(isRemoved()||level!=server)return false;var chunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntities().get(worldPosition)==this;
    }
    @Override public boolean triggerEvent(int event,int argument){if(event==0){initiateSwing();return true;}return false;}
    public void sync(){super.setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return stack.isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?stack:ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack value){if(slot!=0)throw new IndexOutOfBoundsException(slot);stack=value;sync();}
    @Override public ItemStack removeItem(int slot,int count){var result=stack.split(count);sync();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){var result=stack;stack=ItemStack.EMPTY;return result;}
    @Override public boolean canPlaceItem(int slot,ItemStack value){return true;}
    @Override public int getMaxStackSize(){return 64;}
    @Override public void clearContent(){stack=ItemStack.EMPTY;sync();}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public Component getDisplayName(){return Component.translatable("block.thaumictinkerer.animation_tablet");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new AnimationTabletMenu(id,inventory,this);}
    private void saveCustom(CompoundTag tag,HolderLookup.Provider registries){tag.putBoolean("leftClick",leftClick);tag.putBoolean("redstone",redstone);var items=NonNullList.withSize(1,stack);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);saveCustom(tag,registries);tag.putInt("progress",swingProgress);tag.putInt("mod",swingMod);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);if(tag.contains("progress"))swingProgress=tag.getInt("progress");leftClick=tag.getBoolean("leftClick");redstone=tag.getBoolean("redstone");var items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);stack=items.getFirst();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();saveCustom(tag,registries);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
