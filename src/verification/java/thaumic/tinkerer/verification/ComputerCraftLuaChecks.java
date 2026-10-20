package thaumic.tinkerer.verification;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.block.AnimationTabletContent;

/** Loaded only with real CC: ObjectArguments cannot reproduce Lua scope expiry. */
final class ComputerCraftLuaChecks {
    static void run(GameTestHelper helper)throws Exception {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(4,120,4));
        level.setBlock(pos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("computercraft:computer_normal")).defaultBlockState(),3);
        level.setBlock(pos.north(),AnimationTabletContent.BLOCK.get().defaultBlockState(),3);
        var tile=(AbstractComputerBlockEntity)level.getBlockEntity(pos);
        var tablet=(AnimationTabletBlockEntity)level.getBlockEntity(pos.north());
        tile.updateInputsImmediately();var computer=tile.createServerComputer();var mount=computer.createRootMount();
        String program="""
            local ok,err=xpcall(function()
              local t=assert(peripheral.find('tt_animationTablet'),'native tablet discovery')
              t.setRedstone(true); assert(t.getRedstone())
              t.setRedstone(false); assert(not t.getRedstone())
              t.setRedstone(true); t.setLeftClick(false); assert(not t.getLeftClick())
              t.setRotation(3); assert(t.getRotation()==3)
              local valid,message=pcall(t.setRotation,-1)
              assert(not valid and message:find('Invalid value',1,true))
              assert(t.getRotation()==3 and not t.hasItem())
            end,debug.traceback)
            local f=assert(fs.open('tt-result.txt','w'))
            f.write(ok and 'PASS' or tostring(err)); f.close()
            """;
        try(var channel=mount.openFile("startup.lua",Set.of(StandardOpenOption.WRITE,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING))){
            var data=ByteBuffer.wrap(program.getBytes(StandardCharsets.UTF_8));while(data.hasRemaining())channel.write(data);
        }
        computer.turnOn();
        helper.succeedWhen(()->{
            try{
                helper.assertTrue(mount.exists("tt-result.txt"),"Awaiting native CC BIOS and Lua");
                String result;try(var input=Channels.newInputStream(mount.openForRead("tt-result.txt"))){result=new String(input.readAllBytes(),StandardCharsets.UTF_8);}
                helper.assertTrue(result.equals("PASS"),"Actual Lua: "+result);
                helper.assertTrue(tablet.redstone&&!tablet.leftClick&&tablet.reportedRotation()==3,"Actual Lua updated the real tablet");
                level.removeBlock(pos,false);level.removeBlock(pos.north(),false);
                System.out.println("TT_ACTUAL_TABLET_LUA_PASS computer="+computer.getID());
            }catch(java.io.IOException error){throw new GameTestAssertException(error.toString());}
        });
    }
    private ComputerCraftLuaChecks(){}
}
