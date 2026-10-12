<template>
    <div class="random-video">
        <div class="random-video-btn">
            <el-tooltip class="box-item" effect="dark" content="随机视频播放" placement="right">
                <div @click="drawer = true">
                    <el-icon><DArrowRight /></el-icon>
                </div>
            </el-tooltip>
        </div>

        <el-drawer title="随机视频播放" v-model="drawer" :with-header="false">
            <div class="video-container">
                <video ref="video" style="width: 100%" controls autoplay :src="videoSrc"></video>
            </div>
            <div style="margin-left: 20px; margin-top: 10px">
                <el-button type="primary" :icon="btnIcon" size="small" @click="operateVideo">
                    {{ btnContent }}
                </el-button>
                <el-button type="primary" icon="ArrowRight" size="small" @click="nextVideo">下一个视屏</el-button>
            </div>
        </el-drawer>
    </div>
</template>

<script>
export default {
    name: 'RandomVideo',
    data() {
        return {
            drawer: false,
            videoSrc: 'http://api.yujn.cn/api/zzxjj.php',
            isPlaying: true,
            btnContent: '暂停',
            btnIcon: 'VideoPause'
        }
    },
    methods: {
        //切换视频
        nextVideo() {
            this.videoSrc = this.videoSrc + "?temps=" + new Date().getTime();
        },
        //暂停和开启视频
        operateVideo() {
            if (this.isPlaying) {
                this.$refs.video.pause();
                this.btnContent = '播放';
                this.btnIcon = 'VideoPlay';
                this.isPlaying = false;
            } else {
                this.$refs.video.play();
                this.btnContent = '暂停';
                this.btnIcon = 'VideoPause';
                this.isPlaying = true;
            }
        }
    }
}
</script>

<style scoped lang="scss">
.random-video-btn {
    position: fixed;
    left: 20px;
    bottom: 50%;
    font-size: 1.5rem;
    color: $primary;
    cursor: pointer;
}

.video-container {
    padding: $spacing-sm;
    border-radius: $border-radius-md;
    video {
        border-radius: $border-radius-md;
    }
}
</style>
