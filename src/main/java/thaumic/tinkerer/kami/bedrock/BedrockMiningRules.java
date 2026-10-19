package thaumic.tinkerer.kami.bedrock;

/** Release164's literal heights: the surface test is Y<5, not minY+5. */
public final class BedrockMiningRules {
    public enum Change { NONE, PORTAL, AIR }
    public static Change change(boolean enabled,boolean surfaceWorld,boolean bedrockWorld,int y){
        if(!enabled)return Change.NONE;
        // The release uses two independent ifs: AIR wins if a data pack also
        // classifies the bedrock dimension as a surface world.
        if(bedrockWorld&&y<=253)return Change.AIR;
        if(surfaceWorld&&y<5||bedrockWorld&&y>253)return Change.PORTAL;
        return Change.NONE;
    }
    private BedrockMiningRules(){}
}
