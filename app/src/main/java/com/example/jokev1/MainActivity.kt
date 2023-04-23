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
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

class JokeApp:Application() {
    lateinit var viewModel: ViewModel
    override fun onCreate() {
        super.onCreate()
        viewModel=ViewModel(BaseModel(BaseJokeService(),BaseResourceManager(this)))
    }
}

class BaseModel(
    private val service: JokeService,
    private val resourceManager: ResourceManager
    ) : Model {
    private var callback: ResultCallback?=null
    private val noConnection by lazy { NoConnection(resourceManager) }
    private val serviceUnavailable by lazy { ServiceUnavailable(resourceManager) }
    override fun getJoke() {
        service.getJoke(object : ServiceCallback{
            override fun returnSuccess(data: String) {
                callback?.provideSuccess(Joke(data,""))
            }

            override fun returnError(type: ErrorType) {
                when(type){
                    ErrorType.NO_CONNECTION->callback?.provideError(noConnection)
                    ErrorType.OTHER->callback?.provideError(serviceUnavailable)
                }
            }
        })
    }

    override fun init(callback: ResultCallback) {
        this.callback=callback
    }

    override fun clear() {
        callback=null
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

//------------------------------------------------------------------------------------------------
interface JokeService{
    fun getJoke(callback: ServiceCallback)
}

interface ServiceCallback{
    fun returnSuccess(data: String)
    fun returnError(type: ErrorType)
}

enum class ErrorType{
    NO_CONNECTION,
    OTHER
}

class BaseJokeService: JokeService{
    override fun getJoke(callback: ServiceCallback) {
        Thread{
            var connection: HttpURLConnection?=null
            try {
                val url= URL(JOKE_URL)
                connection=url.openConnection() as HttpURLConnection
                InputStreamReader(BufferedInputStream(connection.inputStream)).use {
                    val line:String=it.readText()
                    callback.returnSuccess(line)
                }
            }catch (e:Exception){
                if (e is UnknownHostException)
                    callback.returnError(ErrorType.NO_CONNECTION)
                else
                    callback.returnError(ErrorType.OTHER)
            }finally {
                connection?.disconnect()
            }
        }.start()
    }

    private companion object{
        const val JOKE_URL="https://official-joke-api.appspot.com/random_joke/"
    }
}