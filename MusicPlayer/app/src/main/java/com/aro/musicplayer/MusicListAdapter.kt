package com.aro.musicplayer


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class MusicListAdapter(private var musicList : MutableList<Music>, private var itemClicked : ItemClicked)
    : androidx.recyclerview.widget.RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> () {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): MusicListAdapter.MusicViewHolder {
        val context = viewGroup.context
        val inflater = LayoutInflater.from(context)
        val shouldAttachToParentImmediately = false
        val view = inflater.inflate(R.layout.music_player_item, viewGroup, shouldAttachToParentImmediately)

        return MusicViewHolder(view)
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    override fun onBindViewHolder(holder: MusicListAdapter.MusicViewHolder, position: Int) {
        val item = musicList[position]
        holder.bindMusicData(item)

    }

    inner class MusicViewHolder(v : View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v), View.OnClickListener{
        private var view : View = v
        private lateinit var  music : Music
        private var artistName : TextView = view.findViewById(R.id.artist_name_text_view)
        private var songName : TextView = view.findViewById(R.id.song_name_text_view)


        init {
            view.setOnClickListener(this)
        }

        fun bindMusicData(music : Music){
            this.music = music

            artistName.text = music.artistName
            songName.text = music.songName

        }



        override fun onClick(v: View?) {

            itemClicked.itemClicked(adapterPosition)
        }

    }
}