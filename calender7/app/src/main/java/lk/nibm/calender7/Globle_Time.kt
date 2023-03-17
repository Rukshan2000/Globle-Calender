package lk.nibm.calender7

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class Globle_Time : Fragment() {

    private lateinit var spnCountry: Spinner
    private lateinit var spnYear: Spinner
    private lateinit var spnMonth: Spinner
    private lateinit var holidayRecyclerView: RecyclerView
    private lateinit var locationTextView : TextView
    private lateinit var progressBar: ProgressBar

    var selectedCountry: String = ""
    var selectedYear: String = ""
    var selectedMonth: String = ""

    var holidayDataArray = JSONArray()
    var holidayAdapter: HolidayAdapter? = null

    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val REQUEST_LOCATION_PERMISSION = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_globle__time, container, false)

        holidayRecyclerView = rootView.findViewById(R.id.holidayRecyclerView)
        spnCountry = rootView.findViewById(R.id.spnCountry)
        spnYear = rootView.findViewById(R.id.spnYear)
        locationTextView = rootView.findViewById(R.id.locationTextView)
        spnMonth = rootView.findViewById(R.id.spnMonth)
        progressBar = rootView.findViewById(R.id.progressBar)

        class CountryAdapter(
            context: Context,
            textViewResourceId: Int,
            private val countryList: List<String>
        ) : ArrayAdapter<String>(context, textViewResourceId, countryList) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTypeface(Typeface.DEFAULT_BOLD)
                view.setTextColor(ContextCompat.getColor(context, R.color.light_green))
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)// set text size to 24sp

                // Get the current country item
                val currentItem = getItem(position)

                // Set the text to "Holidays Of + countryName"
                view.text = "Holidays Of $currentItem"
                return view
            }


            // Override the getDropDownView method to customize the appearance of the dropdown items
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                view.setTypeface(Typeface.DEFAULT)
                return view
            }
        }

        // Get the user's current location to determine the country code
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                LOCATION_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is already granted, proceed to access location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations, this can be null.
                    if (location != null) {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        var addresses: List<Address>? = null
                        try {
                            addresses = geocoder.getFromLocation(
                                location.latitude,
                                location.longitude,
                                1
                            )
                            val countryCode = addresses!![0].countryCode
                            locationTextView.text = "Your Current Location " + addresses[0].countryName
                            selectedCountry =
                                countryCode // Set the selected country based on the location
                            getHolidayData(selectedCountry, selectedYear, selectedMonth)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Location not available",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
        } else {
            // Permission is not yet granted, request the permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(LOCATION_PERMISSION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        // Set up country spinner
        val countries = ArrayList<String>()
        countries.add("Sri Lanka")
        val url =
            "https://calendarific.com/api/v2/countries?api_key="+resources.getString(R.string.API)
        val resultCountries = StringRequest(Request.Method.GET, url, Response.Listener { response ->
            try {
                val jsonObject = JSONObject(response)
                val jsonObjectResponse = jsonObject.getJSONObject("response")
                val jsonArrayCountries = jsonObjectResponse.getJSONArray("countries")
                for (i in 0 until jsonArrayCountries.length()) {
                    val jsonObjectCountry = jsonArrayCountries.getJSONObject(i)
                    countries.add(jsonObjectCountry.getString("country_name"))
                }
                val adapterCountry = CountryAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, countries)
                spnCountry.adapter = adapterCountry
                spnCountry.setSelection(countries.indexOf("Sri Lanka"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }, Response.ErrorListener { error ->
            Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
        })
        Volley.newRequestQueue(requireContext()).add(resultCountries)

        spnCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
            {
                getCountryId(spnCountry.selectedItem.toString())



            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Please select a country", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up year spinner
        val years = (2014..2023).map { it.toString() }.reversed().toTypedArray()
        val yearAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)
        spnYear.adapter = yearAdapter

        spnYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedYear = years[position]
                getHolidayData(selectedCountry, selectedYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Please select year", Toast.LENGTH_SHORT).show()            }

        }

        // Set up month spinner
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
        )
        val monthAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spnMonth.adapter = monthAdapter

        spnMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedMonth = (position + 1).toString()
                getHolidayData(selectedCountry, selectedYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

                    Toast.makeText(requireContext(), "Please select month", Toast.LENGTH_SHORT).show()            }
        }

        holidayRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL, false
        )

        holidayRecyclerView.adapter = HolidayAdapter()
        return rootView
    }

    private fun getCountryId(name: String) {
        val url = "https://calendarific.com/api/v2/countries?api_key="+resources.getString(R.string.API)
        val resultCountries = StringRequest(Request.Method.GET, url, Response.Listener { response ->
            try {
                val jsonObject = JSONObject(response)
                val jsonObjectResponse = jsonObject.getJSONObject("response")
                val jsonArrayCountries = jsonObjectResponse.getJSONArray("countries")
                for (i in 0 until jsonArrayCountries.length()) {
                    val jsonObjectCountry = jsonArrayCountries.getJSONObject(i)
                    if (jsonObjectCountry.getString("country_name") == name) {
                        selectedCountry = jsonObjectCountry.getString("iso-3166").toString()
                        getHolidayData(selectedCountry, selectedYear, selectedMonth)

                        break
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }, Response.ErrorListener { error ->
            Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
        })
        Volley.newRequestQueue(requireContext()).add(resultCountries)

    }

    private fun getHolidayData(countryName: String, year: String, month: String) {

        progressBar.visibility = View.VISIBLE // show the progress bar

        val url =
            "https://calendarific.com/api/v2/holidays?&api_key="+resources.getString(R.string.API)+"&country=" + countryName + "&year=" + year + "&month=" + month + ""

        val request = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val jsonObjectResponse = jsonObject.getJSONObject("response")
                    holidayDataArray = jsonObjectResponse.getJSONArray("holidays")
                    holidayRecyclerView.adapter?.notifyDataSetChanged()

                    progressBar.visibility = View.GONE // hide the progress bar

                } catch (e: Exception) {
                    e.printStackTrace()
                    progressBar.visibility = View.GONE // hide the progress bar

                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                progressBar.visibility = View.GONE // hide the progress bar

            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    inner class HolidayAdapter : RecyclerView.Adapter<HolidayViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.holiday_row, parent, false)

            return HolidayViewHolder(view)
        }

        override fun onBindViewHolder(holder: HolidayViewHolder, position: Int) {
            try {
                val holiday = holidayDataArray.getJSONObject(position)

                holder.holidayTitle.text = holiday.getString("name")
                holder.holidayType.text = holiday.getString("primary_type")
                holder.holidayDate.text =
                    holiday.getJSONObject("date").getJSONObject("datetime").getInt("day").toString()

                holder.itemView.setOnClickListener {
                    val dialogBuilder = AlertDialog.Builder(holder.itemView.context)
                    dialogBuilder.setTitle(holiday.getString("name"))
                    dialogBuilder.setMessage(holiday.getString("description"))
                    dialogBuilder.setPositiveButton("OK", null)
                    dialogBuilder.create().show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun getItemCount(): Int {
            return holidayDataArray.length()
        }
    }

    inner class HolidayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val holidayTitle: TextView = itemView.findViewById(R.id.holidayName)
        val holidayType: TextView = itemView.findViewById(R.id.holidayType)
        val holidayDate: TextView = itemView.findViewById(R.id.holidayDate)
    }
}
