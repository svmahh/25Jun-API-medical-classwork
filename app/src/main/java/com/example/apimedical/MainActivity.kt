package com.example.apimedical

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.gson.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.concurrent.Executors
import com.github.kittinunf.result.Result

class MainActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var outputTextView: TextView
    private lateinit var inputIdEditText: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        outputTextView = findViewById(R.id.txtOutput)
        inputIdEditText = findViewById(R.id.etInputId)

        findViewById<Button>(R.id.btnGetAll).setOnClickListener {
            hideKeyboard()
            getAllLoans()
        }

        findViewById<Button>(R.id.btnGetAllById).setOnClickListener {
            hideKeyboard()
            val idText = inputIdEditText.text.toString()
            if (idText.isNotEmpty()){
                try{
                    //try to convert the input to an integer
                    val id = idText.toInt()
                    getLoanbyID(id)
                }
                catch (e: NumberFormatException){
                    // handle cases where the input is not a valid number.
                    Toast.makeText(this, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnCreate).setOnClickListener {
            hideKeyboard()
            createNewLoan()
        }

    }

    private fun getAllLoans() {
        // Define the API endpoint URL
        val url = "https://opsc.azurewebsites.net/loans/"
        outputTextView.text = "Fetching All loans..."

        //execute the network request on a background thread
        executor.execute {
            url.httpGet().responseString { _, _, result ->
                //switch to the main thread to update the UI
                handler.post {
                    when (result) {
                        is com.github.kittinunf.result.Result.Success -> {
                            //on success , deserialize json string into a list of json objects
                            val json = result.get()
                            try {
                                val loans = Gson().fromJson(json, Array<Loan>::class.java).toList()
                                if (loans.isNotEmpty()) {
                                    //format the output for readibility.
                                    val formattedOutput =
                                        loans.joinToString(separator = "\n\n") { loan ->
                                            "Loan ID: ${loan.LoanID}\nAmount: ${loan.amount}\nMember ID: " +
                                                    "${loan.memberID}\nMessage: ${loan.message}"
                                        }
                                    outputTextView.text = formattedOutput
                                } else {
                                    outputTextView.text = "No Loans found"
                                }
                            } catch (e: JsonSyntaxException) {
                                // Handle cases where the server response is not valid JSON
                                Log.e("GetAllLoans", "JSON parsing error: ${e.message}")
                                outputTextView.text = "could not parse server response."
                            }

                        }

                        is Result.Failure -> {
                            //on failure, log the error
                            val ex = result.getException()
                            Log.e("GetAllLoans", "API Error: ${ex.message}")
                            outputTextView.text = "Error: Could not fetch loans from the server"
                        }

                    }


                }
            }
        }
    }

    private fun getLoanbyID(id:Int){
        // Define the API endpoint URL
        val url = "https://opsc.azurewebsites.net/loans/$id"
        outputTextView.text = "Fetching loan with ID : $id..."

        //execute the network request on a background thread
        executor.execute {
            url.httpGet().responseString { _, response, result ->
                //switch to the main thread to update the UI
                handler.post {
                    if (response.statusCode == 404){
                        outputTextView.text = "Loan with ID $id not found"
                        return@post
                    }

                    when (result) {
                        is Result.Success ->{
                            try{
                            val loan = Gson().fromJson(result.get(), Loan::class.java)
                            val formattedOutput = "Loan ID: ${loan.LoanID}\nAmount: ${loan.amount}"+
                                    "${loan.memberID}\nMessage: ${loan.message}"
                            outputTextView.text = formattedOutput
                        }
                        catch (e: JsonSyntaxException) {
                                // Handle cases where the server response is not valid JSON
                                Log.e("GetLoanbyID", "JSON parsing error: ${e.message}")
                                outputTextView.text = "could not parse server response."
                            }
                        }

                        is Result.Failure -> {
                            //on failure, log the error
                            val ex = result.getException()
                            Log.e("GetAllLoans", "API Error: ${ex.message}")
                            outputTextView.text = "Error: Could not fetch loans from the server"
                        }
                    }
                }
            }
        }
    }

    // homework
    private fun getLoansByMemberID(memberId:String){
       val url = "https://opsc.azurewebsites.net/loans/member/$memberId"
        outputTextView.text = "Fetching loans for member with ID : $memberId..."

    }


    private fun createNewLoan() {
        val url = "https://opsc.azurewebsites.net/loans/"
        outputTextView.text = "creating a new loan..."
        executor.execute {
            //create a new loan object to send as the request body.
            val newLoan = LoanPost("15.99", "M6001", "Added by the android app")
            val jsonBody = Gson().toJson(newLoan)

            url.httpPost()
                .jsonBody(jsonBody) // set the request body
                .responseString { _, response, result ->
                    handler.post {
                        when (result) {
                            // a 201 created status code indicates success
                            is Result.Success -> {
                                if (response.statusCode == 201) {
                                    try {
                                        val createdLoan = Gson().fromJson(
                                            result.get(),
                                            Loan::class.java
                                        )

                                        outputTextView.text =
                                            "Successfully Created loan: \n\nLoan ID:" +
                                                    "${createdLoan.LoanID}\n AMount: ${createdLoan.amount}"

                                    } catch (e: JsonSyntaxException) {
                                        Log.e("CreateNewLoan", "JSON parsing error: ${e.message}")
                                        outputTextView.text = "could not parse server response."
                                    }
                                    } else {
                                        outputTextView.text =
                                            "Error: Could not create loan. status: ${response.statusCode}"
                                    }

                                }

                                is Result.Failure -> {
                                    val ex = result.getException()
                                    Log.e("CreateNewLoan", "API Error: ${ex.message}")
                                    outputTextView.text =
                                        "Error: Could not create loan. status: ${response.statusCode}"
                                }
                            }

                        }
                    }
                }

        }



    private fun hideKeyboard(){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

    }

}