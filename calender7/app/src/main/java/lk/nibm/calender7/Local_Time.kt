package lk.nibm.calender7

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*



class Local_Time : Fragment() {

    private lateinit var spnYear: Spinner
    private lateinit var spnMonth: Spinner
    private lateinit var holidayRecyclerView: RecyclerView
    private lateinit var locationTextView : TextView
    private lateinit var progressBar: ProgressBar

    var selectedCountry: String = ""
    var selectedYear: String = ""
    var selectedMonth: String = ""

    var holidayDataArray = JSONArray()

    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val REQUEST_LOCATION_PERMISSION = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootview = inflater.inflate(R.layout.fragment_local__time, container, false)

        holidayRecyclerView = rootview.findViewById(R.id.holidayRecyclerView)
        spnYear = rootview.findViewById(R.id.spnYear)
        spnMonth = rootview.findViewById(R.id.spnMonth)
        locationTextView = rootview.findViewById(R.id.locationTextView)
        progressBar = rootview.findViewById(R.id.progressBar)


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
                            locationTextView.text = "Welcome to: " + addresses[0].countryName
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

        // Set up year spinner
        val years = (2016..2023).map { it.toString() }.reversed().toTypedArray()
        val yearAdapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, years)
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
            ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, months)
        spnMonth.adapter = monthAdapter

        spnMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedMonth = (position + 1).toString().padStart(2, '0')
                getHolidayData(selectedCountry, selectedYear, selectedMonth)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Toast.makeText(requireContext(), "Please select a month", Toast.LENGTH_SHORT).show()            }
        }

        // Set up RecyclerView
        holidayRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity()

        )

        holidayRecyclerView.adapter = HolidayAdapter()
        return rootview
    }

    private fun getHolidayData(countryCode: String, year: String, month: String) {
        progressBar.visibility = View.VISIBLE // show the progress bar

        val url =
            "https://calendarific.com/api/v2/holidays?&api_key="+resources.getString(R.string.API)+"&country=" + countryCode + "&year=" + year + "&month=" + month + ""

        val request = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val jsonObjectResponse = jsonObject.getJSONObject("response")
                    holidayDataArray = jsonObjectResponse.getJSONArray("holidays")
                    holidayRecyclerView.adapter?.notifyDataSetChanged()

                    // Set country and city name
                    val geocoder = Geocoder(requireActivity(), Locale.getDefault())
                    val addresses: List<Address>? = geocoder.getFromLocation(
                        holidayDataArray.getJSONObject(0).getJSONObject("country").getDouble("lat"),
                        holidayDataArray.getJSONObject(0).getJSONObject("country").getDouble("lng"),
                        1
                    )
                    if (addresses != null && addresses.isNotEmpty()) {

                        val countryName = addresses[0].countryName
                        val cityName = addresses[0].locality
                        locationTextView.text = "$cityName, $countryName"
                    }
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
