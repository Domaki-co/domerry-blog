<template>
  <div>
    <div id="aplayer" v-show="audio.length>0"></div>
  </div>
</template>

<script>
import {getSongList} from "../api/file"
import APlayer from 'aplayer'
import 'aplayer/dist/APlayer.min.css'
import axios from "axios";
import { decodeText } from '@/utils/encoding'

export default {
  name: "Player",
  data(){
    return {
      audio: [],
      isLoading: true,
      player: null,
      errorHandler: null,
      lyricPlayHandler: null
    }
  },
  async created() {
    try {
      const resp = await getSongList();
      if (!resp.data || resp.data.length === 0) {
        console.warn('No songs found in the list');
        return;
      }
      // 歌词不随歌单一次性下载，只保留地址，真正播放到某首歌时才按需请求
      const processedData = resp.data.map((item) => {
        if (item.cover === null || item.cover === undefined || item.cover === '') {
          item.cover = 'https://avatars2.githubusercontent.com/u/20062482?s=270'
        }
        item.lrcUrl = item.lrc || '';
        item.lrc = '';
        item.lrcLoading = false;
        return item;
      });
      this.audio = processedData;

      // 在下一个 tick 中初始化 APlayer，确保 DOM 已经更新
      this.$nextTick(() => {
        this.initAPlayer();
      });
    } catch (error) {
      console.error('Error loading song list:', error);
    } finally {
      this.isLoading = false;
    }
  },
  methods: {
    initAPlayer() {
      if (this.audio.length === 0) return;

      // 如果已经存在播放器实例，先销毁它
      if (this.player) {
        this.player.destroy();
      }

      // 创建新的播放器实例
      this.player = new APlayer({
        container: document.getElementById('aplayer'),
        fixed: true,
        lrcType: 1,
        autoplay: false,
        theme: '#42b983',
        listFolded: false,
        audio: this.audio
      });

      if (this.player.lrc) {
        this.player.lrc.hide();
        this.player.template.lrcButton.classList.add('aplayer-icon-lrc-inactivity');
      }

      // 监听播放错误：避免循环重试，报错即暂停
      this.bindPlayerErrorHandler();
      // 播放时才加载当前歌曲的歌词
      this.bindLyricLoader();
    },
    bindLyricLoader() {
      if (!this.player || this.lyricPlayHandler) return;
      this.lyricPlayHandler = () => {
        this.loadCurrentLyric();
      };
      this.player.on('play', this.lyricPlayHandler);
    },
    async loadCurrentLyric() {
      const player = this.player;
      if (!player || !player.lrc) return;

      const index = player.list.index;
      const song = this.audio[index];
      // 没有歌词地址、已加载或正在加载时跳过
      if (!song || !song.lrcUrl || song.lrc || song.lrcLoading) return;

      song.lrcLoading = true;
      try {
        const response = await axios.get(song.lrcUrl, {
          responseType: 'arraybuffer'
        });
        const contentType = response.headers['content-type'] || '';
        const charsetMatch = contentType.match(/charset=["']?([^;"'\s]+)/i);
        song.lrc = decodeText(response.data, charsetMatch ? charsetMatch[1] : 'utf-8');
      } catch (error) {
        console.error(`Error loading lyrics for song: ${song.name}`, error);
      } finally {
        song.lrcLoading = false;
      }

      if (!song.lrc) {
        // 加载失败或空歌词，置空地址避免每次播放都重复请求
        song.lrcUrl = '';
        return;
      }

      // APlayer 切歌时已把空歌词缓存进 lrc.parsed，这里清掉让它按新歌词重新解析
      if (player.lrc.parsed) {
        player.lrc.parsed[index] = undefined;
      }
      if (player.list.index === index) {
        player.lrc.switch(index);
        player.lrc.update();
      }
    },
    bindPlayerErrorHandler() {
      const audio = this.player && this.player.audio
      if (!audio) return
      if (this.errorHandler) return

      this.errorHandler = (event) => {
        try {
          event.stopImmediatePropagation()
        } catch (e) { /* ignore */ }
        if (this.player) {
          this.player.pause()
          this.player.seek(0)
        }
        // this.$message && this.$message.warning('音频播放出错，已暂停')
      }
      // 用捕获阶段提前拦截 error 事件，阻止 APlayer 内部自动切歌/重试
      audio.addEventListener('error', this.errorHandler, true)
    },
  },
  beforeUnmount() {
    // 组件销毁前清理播放器实例
    if (this.player) {
      const audio = this.player.audio
      if (audio && this.errorHandler) {
        audio.removeEventListener('error', this.errorHandler, true)
      }
      this.player.destroy();
      this.player = null;
      this.errorHandler = null;
    }
  }
}
</script>

<style>
/* 确保播放器在固定模式下正确显示 */
.aplayer.aplayer-fixed {
  max-width: 400px;
  margin: 0;
  z-index: 999;
}
</style>
