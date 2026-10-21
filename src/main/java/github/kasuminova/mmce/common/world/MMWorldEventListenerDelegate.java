package github.kasuminova.mmce.common.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IWorldAccess;

/**
 * 1.7.10 移植：IWorldAccess 的空实现基类，子类只覆盖需要的回调。
 */
public abstract class MMWorldEventListenerDelegate implements IWorldAccess {

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    @Override
    public void playSound(String soundName, double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String soundName, double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void spawnParticle(String particleName, double x, double y, double z, double velX, double velY, double velZ) {
    }

    @Override
    public void onEntityCreate(Entity entity) {
    }

    @Override
    public void onEntityDestroy(Entity entity) {
    }

    @Override
    public void playRecord(String recordName, int x, int y, int z) {
    }

    @Override
    public void broadcastSound(int soundID, int x, int y, int z, int data) {
    }

    @Override
    public void playAuxSFX(EntityPlayer player, int sfxType, int x, int y, int z, int data) {
    }

    @Override
    public void destroyBlockPartially(int breakerId, int x, int y, int z, int progress) {
    }

    @Override
    public void onStaticEntitiesChanged() {
    }
}
