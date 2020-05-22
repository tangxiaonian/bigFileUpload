<template>
  <div id="app">
    <input type="file" name="file" @change="changeClick" ref="file">
    <button @click="handlerClick" v-preventReClick="2000">btn</button>

    <hr>

    <video controls ref="myVideo"></video>

<!--    <video src="http://localhost:8081/test/getUpload?id=51b863ea0e3cc9c905f23ddd13d296af"-->
<!--           width="400" height="300"-->
<!--           controls></video>-->

  </div>
</template>

<script>
  export default {
    name: 'App',
    data () {
      return {
        param: {
          chunkSize: 0, // 每一个分片的大小
          chunk: 1, // 当前是第几个分片
          taskId: '', // 任务id
          countChunk: 10 // 总分片数
        },
        chunkStart: 1, // 当前上传到了第几个分片
        fileSize: 0, // 文件的大小
        requestList:[], // 要上传的分片列表
        sparkMd5: null,
        mediaSource: null,
        mimeCode:""  // 文件的mime类型
      }
    },
    mounted () {
      this.sparkMd5 = new this.$sparkMd5()
    },
    methods: {
      handlerClick () {
        // this.$http.get('http://localhost:8081/test/upload1?id=51b863ea0e3cc9c905f23ddd13d296af',{
        //   responseType: "arraybuffer"
        // }).then((response) => {
        //
        // })
        this.createVideo()
      },
      createVideo () {
        let video = this.$refs.myVideo
        this.mimeCode = 'video/mp4; codecs="avc1.42E01E, mp4a.40.2"'
        if ('MediaSource' in window && MediaSource.isTypeSupported(this.mimeCode)) {
          this.mediaSource = new MediaSource()
          video.src = URL.createObjectURL(this.mediaSource)
          this.mediaSource.addEventListener('sourceopen', this.sourceOpen)
        }
      },
      sourceOpen () {
        let sourceBuffer = this.mediaSource.addSourceBuffer(this.mimeCode)
        this.fetchSource((buffer) => {
          // 调用 appendBuffer 方法后触发。。。
          this.mediaSource.addEventListener("updateend",() => {
            console.log( "updateend...." )
            this.mediaSource.endOfStream()
            this.$refs.myVideo.play()
          });
          // 添加二进制流
          sourceBuffer.appendBuffer(buffer)
        })
      },
      fetchSource (cb) {
        this.$http.get('http://localhost:8081/test/getUpload?id=51b863ea0e3cc9c905f23ddd13d296af',{
          responseType: "arraybuffer"
        }).then((response) => {
          cb(response.data)
        });
      },
      /**
       * 开始上传文件
       */
      upLoadFile () {
        // 获取MD5加密字符串
        let hexHash = this.sparkMd5.end()
        if (this.requestList.length > 0) {
          this.requestList.forEach((item) => {
            item.append('taskId', hexHash)
          })
        }
        // 开始文件上传
        let requests = []
        this.requestList.forEach((data) => {
          requests.push(new Promise(((resolve, reject) => {
            this.$http.post('http://localhost:8081/test/upload', data)
              .then(resolve).catch(reject)
          })));
        });
        Promise.all( requests ).then((values) => {
          console.log(values)
        });
      },
      /**
       * 文件选择后，触发
       */
      changeClick () {
        let file = this.$refs.file.files[0]
        this.fileSize = file.size
        if (file) {
          // 每一个分片的大小
          this.param.chunkSize = file.size / this.param.countChunk
          // 开始分片
          this.nextLoad(file, new FileReader())
        }
      },
      nextLoad (file, fileReader) {
        // 读取操作完成时触发
        fileReader.onload = () => {
          // 分片位置+1
          this.chunkStart++
          // 当前分片  《=   总片数   ===》  继续上传分片
          if (this.param.chunk <= this.param.countChunk) {
            console.log('上传当前分片为：' + (this.chunkStart - 1))
            this.param.chunk++
            this.nextLoad(file, new FileReader())
          }
          // 文件读取状态为 完成  加密 文件
          if (fileReader.readyState === FileReader.DONE) {
            // 分片上传
            let file = fileReader.result;
            // 加密
            this.sparkMd5.append(file);
          }
        }
        if (this.param.chunk <= this.param.countChunk) {
          // 计算 开始 和 结束位置
          let start = (this.chunkStart - 1) * this.param.chunkSize
          let end = this.chunkStart * this.param.chunkSize
          // 需要判断是不是 文件末尾
          if (this.param.chunk === this.param.countChunk) {
            end = file.size
          }
          console.log('start=', start, 'end=', end)
          // 切割文件
          let chunkFile = file.slice(start, end)
          let formData = new FormData();
          formData.append('file', chunkFile)
          formData.append('chunk', this.param.chunk)
          formData.append('chunkTotal', this.param.countChunk)
          formData.append('size', this.param.chunkSize)
          // 放到待请求列表中
          this.requestList.push(formData);
          fileReader.readAsArrayBuffer(chunkFile)
        }else {
          console.log('开始文件上传...')
          setTimeout(() => {
            this.upLoadFile()
          },2000)
        }
      }
    }
  }
</script>

<style>
  #app {
    font-family: Avenir, Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
  }
</style>
