package com.aro.musicplayer


import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MusicPlayerActivity : AppCompatActivity(), ItemClicked {

    /*
  Music app populates with all music from external storage.


   */


    override fun itemClicked(position: Int) {
        stop()
        this.currPosition = position
        if(isShuffling){
            isShuffling = false
            play(position)
            isShuffling = true
        }
        else{
            play(position)
        }
    }

    private lateinit var musicList : MutableList<Music>
    private lateinit var linearLayoutManager : LinearLayoutManager
    private lateinit var adapter : MusicListAdapter
    private lateinit var mAdView : AdView
    private lateinit var playButton : FloatingActionButton
    private lateinit var nextButton : FloatingActionButton
    private lateinit var previousButton : FloatingActionButton
    private lateinit var shuffleButton : FloatingActionButton
    private lateinit var seekbar : SeekBar
    private lateinit var songTitleTextViewInCardView : TextView
    private lateinit var timeElapsedTextView : TextView
    private lateinit var timeRemainingTextView : TextView
    private lateinit var recyclerView : RecyclerView
    private lateinit var cardView : CardView

    private var currPosition : Int = 0
    private var isPlaying = false
    private var isShuffling = false
    private var mediaPlayer : MediaPlayer? = null
    private var randomPosition : Int = 0
    private var songPercent : Int = 0


    companion object{
        private const val  REQUEST_CODE_READ_EXTERNAL_STORAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        musicList = mutableListOf()


        //this make the banner ad show in the ad view at the bottom of the layout.
        MobileAds.initialize(this){}
        val adRequest : AdRequest = AdRequest.Builder().build()

        mAdView = findViewById(R.id.ad_view)
        mAdView.loadAd(adRequest)

        playButton = findViewById(R.id.play_button)
        nextButton = findViewById(R.id.next_button)
        previousButton = findViewById(R.id.previous_button)
        shuffleButton = findViewById(R.id.shuffle_button)
        seekbar = findViewById(R.id.seek_bar)

        songTitleTextViewInCardView = findViewById(R.id.song_title_text_view_in_card_view)
        timeElapsedTextView = findViewById(R.id.time_elapsed_text_view)
        timeRemainingTextView = findViewById(R.id.time_remaining_text_view)

        recyclerView = findViewById(R.id.recycler_view)
        cardView = findViewById(R.id.card_view)

        checkPermissions()

        //don't let screen go to sleep. going to sleep mid-song is bad UX
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playButton.setOnClickListener { play(currPosition) }

        nextButton.setOnClickListener { next() }

        previousButton.setOnClickListener { previous() }

        shuffleButton.setOnClickListener { shufflePlay() }



        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser){
                    if(mediaPlayer != null){
                        mediaPlayer?.seekTo( progress * 1000)
                    }
                }
                songPercent = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }




     fun next (){
        if(isShuffling){
            stop()
            randomPosition = Random.nextInt(0, musicList.size)
            play(randomPosition)
        } else if(currPosition < musicList.size - 1) {
            stop()
            currPosition += 1
            play(currPosition)
        } else {
            stop()
        }
    }

    private fun previous (){
        if(isShuffling){
            stop()
            randomPosition = Random.nextInt(0, musicList.size)
            play(randomPosition)
        } else if(currPosition > 0) {
            stop()
            currPosition -= 1
            play(currPosition)
        } else {
            stop()
            play(currPosition)
        }
    }

    private fun stop (){
        if(mediaPlayer!=null){
            mediaPlayer?.stop()
            isPlaying = false
            songPercent = 0
            playButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow, null))
            songTitleTextViewInCardView.text = " "
        }
    }

    private fun pause(){
        if(mediaPlayer != null){
            mediaPlayer?.pause()
            isPlaying = false
            playButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_play_arrow, null))

        }
    }



    private fun shufflePlay(){

        //toggle shuffle

        if(!isShuffling){
            shuffleButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_shuffle_on, null))
            isShuffling= true
        }
        else{
            shuffleButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_shuffle, null))
            isShuffling= false
        }

        //if it is not currently playing a song or midway through a song then start playing a random song
        if(!isPlaying && songPercent == 0){
            //random position will be picked inside play when shuffle is on
            stop()
            play(currPosition)
        }

    }

    private fun play(currPosition : Int){



        var position = currPosition

        //if it is not currently playing and not midway through a song
        if(!isPlaying && songPercent == 0){

            playButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null))
            isPlaying = true

            if(isShuffling){
                randomPosition = Random.nextInt(0, musicList.size)
                position = randomPosition
            }


            mediaPlayer = MediaPlayer().apply {

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(this@MusicPlayerActivity, Uri.parse(musicList[position].songUri))
                prepare()
                start()

                songTitleTextViewInCardView.text = musicList[position].songName

            }

            val mHandler = Handler()

            //get song info and set autoplay next at run time
            if(mediaPlayer != null){
                this@MusicPlayerActivity.runOnUiThread(object : Runnable{
                    override fun run() {
                        val playerPosition = mediaPlayer?.currentPosition!! / 1000
                        val totalDuration = mediaPlayer?.duration!! / 1000

                        seekbar.max = totalDuration
                        seekbar.progress = playerPosition


                        timeElapsedTextView.text = timerFormat(playerPosition.toLong())
                        timeRemainingTextView.text = timerFormat((totalDuration - playerPosition).toLong())

                        mHandler.postDelayed(this, 1000)

                        if (playerPosition == totalDuration){

                            next()

                        }
                    }
                })
            }

        }
        //if it is paused midway through a song
        else if(!isPlaying && songPercent > 0){
            //resources.getDrawable(R.drawable.ic_pause, null
            playButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_pause, null))
            isPlaying = true

            //pause keeps the position in the player so we can just start
            mediaPlayer?.start()


            val mHandler = Handler()

            if(mediaPlayer != null){
                //get song info and set autoplay next at run time
                this@MusicPlayerActivity.runOnUiThread(object : Runnable{
                    override fun run() {
                        val playerPosition = mediaPlayer?.currentPosition!! / 1000
                        val totalDuration = mediaPlayer?.duration!! / 1000

                        seekbar.max = totalDuration
                        seekbar.progress = playerPosition


                        timeElapsedTextView.text = timerFormat(playerPosition.toLong())
                        timeRemainingTextView.text = timerFormat((totalDuration - playerPosition).toLong())

                        mHandler.postDelayed(this, 1000)

                        if (playerPosition == totalDuration){

                            next()

                        }
                    }
                })
            }

        }
        //if currently playing
        else{
            pause()
        }
    }

    private fun timerFormat(time : Long) : String {

        val result = String.format("%02d:%02d",
            TimeUnit.SECONDS.toMinutes(time) ,
            TimeUnit.SECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(time)))

        var convert = " "

        for (element in result)
            convert += element

        return convert
    }

    private fun getSongs(){

        val selection = MediaStore.Audio.Media.IS_MUSIC
        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA)

        val cursor : Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null)

        while (cursor!!.moveToNext()){
            musicList.add(Music(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2)))
        }
        cursor.close()

        musicList.sortBy { it.artistName.lowercase() }

        linearLayoutManager = LinearLayoutManager(this)
        adapter = MusicListAdapter(musicList, this)

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter

        if(musicList.size == 0){
            Snackbar.make(cardView, "No music found on external storage", Snackbar.LENGTH_LONG)
                .show()
        }

    }


    private fun checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            getSongs()
        }else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                val appName = getString(R.string.app_name)
                Toast.makeText(this, "$appName needs access to your external storage", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when (requestCode) {
            REQUEST_CODE_READ_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSongs()
            } else {
                Toast.makeText(this, "Permission is not granted", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onStop() {
        super.onStop()
        pause()
    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null

    }
}
