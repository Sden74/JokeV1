package com.example.jokev1

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

class JokeApp:Application() {
    lateinit var viewModel: ViewModel
    override fun onCreate() {
        super.onCreate()
        viewModel=ViewModel(TestModel())
    }
}

interface TextCallback{
    fun provideText(text:String)
}

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel:ViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel=(application as JokeApp).viewModel
        val button=findViewById<Button>(R.id.actionButton)
        val progressBar=findViewById<ProgressBar>(R.id.progressBar)
        val textView=findViewById<TextView>(R.id.textView)
        progressBar.visibility= View.INVISIBLE

        button.setOnClickListener{
            button.isEnabled=false
            progressBar.visibility=View.VISIBLE
            viewModel.getJoke()
        }

        viewModel.init(object :TextCallback{
            override fun provideText(text: String) = runOnUiThread{
                button.isEnabled=true
                progressBar.visibility=View.VISIBLE
                textView.text=text
            }
        })
    }

    override fun onDestroy() {
        viewModel.clear()
        super.onDestroy()
    }
}

class ViewModel(private val model:Model<Any,Any>){
    private var callback:TextCallback?=null

    fun init(callback: TextCallback){
        this.callback=callback
        model.init(object : ResultCallback<Any,Any>{
            override fun provideSuccess(data: Any) {
                callback.provideText(data.toString())
            }

            override fun provideError(error: Any) {
                callback.provideText(error.toString())
            }
        })
    }
    fun getJoke(){
        model.getJoke()
    }
    fun clear(){
        callback=null
        model.clear()
    }
}

interface Model<S,E> {
    fun init(callback: ResultCallback<S,E>)
    fun getJoke()
    fun clear()
}
interface ResultCallback<S,E>{
    fun provideSuccess(data: S)
    fun provideError(error: E)
}

class TestModel:Model<Any,Any>{
    private var callback:ResultCallback<Any,Any>?=null
    private var count=1

    override fun init(callback: ResultCallback<Any, Any>) {
        this.callback=callback
    }
    /*override fun getJoke() {
        Thread.sleep(1000)
        if (count%2==0){
            callback?.provideSuccess("success")
        }else{
            callback?.provideError("error")
        }
        count++
    }*/
    override fun getJoke() {
        Thread {
            Thread.sleep(1000)
            if (count % 2 == 0) {
                callback?.provideSuccess("success")
            } else {
                callback?.provideError("error")
            }
            count++
        }.start()
    }
    override fun clear() {
        this.callback=null
    }
}