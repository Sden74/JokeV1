package com.example.jokev1

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes

class JokeApp:Application() {
    lateinit var viewModel: ViewModel
    override fun onCreate() {
        super.onCreate()
        viewModel=ViewModel(TestModel(BaseResourceManager(this)))
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

class ViewModel(private val model:Model){
    private var callback:TextCallback?=null

    fun init(callback: TextCallback){
        this.callback=callback
        model.init(object : ResultCallback{
            override fun provideSuccess(data: Joke) {
                callback.provideText(data.getJokeUi())
            }

            override fun provideError(error: JokeFailure) {
                callback.provideText(error.getMessage())
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

interface Model{
    fun init(callback: ResultCallback)
    fun getJoke()
    fun clear()
}
interface ResultCallback{
    fun provideSuccess(data: Joke)
    fun provideError(error: JokeFailure)
}

class TestModel(resourceManager: ResourceManager):Model{
    private var callback:ResultCallback?=null
    private var count=1
    private val noConnection=NoConnection(resourceManager)
    private val serviceUnavailable=ServiceUnavailable(resourceManager)

    override fun init(callback: ResultCallback) {
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
            when(count){
                0->callback?.provideSuccess(Joke("testText", "testPunchline"))
                1->callback?.provideError(noConnection)
                2->callback?.provideError(serviceUnavailable)
            }
            count++
            if (count==3) count=0
        }.start()
    }
    override fun clear() {
        this.callback=null
    }
}
//---------------------------------------------------------------------------------------------
class Joke(private val text: String, private val punchline: String){
    fun getJokeUi()="$text\n$punchline"
}

interface JokeFailure{
    fun getMessage():String
}

class NoConnection(private val resourceManager: ResourceManager):JokeFailure{
    override fun getMessage(): String {
        return resourceManager.getString(R.string.no_connection)
    }
}

class ServiceUnavailable(private val resourceManager: ResourceManager):JokeFailure{
    override fun getMessage(): String {
        return resourceManager.getString(R.string.service_unavailable)
    }
}

interface ResourceManager{
    fun getString(@StringRes stringResId: Int):String
}

class BaseResourceManager(private val context:Context):ResourceManager{
    override fun getString(stringResId: Int): String {
        return context.getString(stringResId)
    }
}