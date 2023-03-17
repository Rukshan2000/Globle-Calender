package lk.nibm.calender7

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Home : Fragment() {

    private lateinit var holidayRecyclerView: RecyclerView
    private lateinit var locationTextView : TextView
    private lateinit var progressBar: ProgressBar
    lateinit var calendarView: CalendarView




    var selectedCountry: String = ""
    var holidayDataArray = JSONArray()

    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    private val REQUEST_LOCATION_PERMISSION = 100
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        holidayRecyclerView = rootView.findViewById(R.id.holidayRecyclerView)
        locationTextView = rootView.findViewById(R.id.locationTextView)
        progressBar = rootView.findViewById(R.id.progressBar)
        calendarView = rootView.findViewById(R.id.calendar)



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
                            locationTextView.text =""+addresses[0].countryName
                            selectedCountry =
                                countryCode // Set the selected country based on the location
                            getHolidayData(selectedCountry)
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
        }else {
            // Permission is not yet granted, request the permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(LOCATION_PERMISSION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        holidayRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL, false
        )
        holidayRecyclerView.adapter = HolidayAdapter()

        // Initialize the TextView for the clock
        val clock = rootView.findViewById<TextView>(R.id.clock)

        // Create a handler to update the clock every second
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                // Get the current time and format it
                val calendar = Calendar.getInstance()
                val formatter = SimpleDateFormat("hh:mm a") // Use "hh" and "a" for 12-hour format
                val formattedTime = formatter.format(calendar.time)

                // Update the clock TextView
                clock.text = formattedTime

                // Schedule the next update in one second
                handler.postDelayed(this, 1000)
            }
        })

        val morningImage = rootView.findViewById<ImageView>(R.id.morningClipArt)
        val afternoonImage = rootView.findViewById<ImageView>(R.id.afternoonClipArt)
        val eveningImage = rootView.findViewById<ImageView>(R.id.eveningClipArt)
        val nightImage = rootView.findViewById<ImageView>(R.id.nightClipArt)

        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        when (hourOfDay) {
            in 6..11 -> {
                // Show morning image
                morningImage.visibility = View.VISIBLE
                afternoonImage.visibility = View.GONE
                eveningImage.visibility = View.GONE
                nightImage.visibility = View.GONE
            }
            in 12..16 -> {
                // Show afternoon image
                morningImage.visibility = View.GONE
                afternoonImage.visibility = View.VISIBLE
                eveningImage.visibility = View.GONE
                nightImage.visibility = View.GONE
            }
            in 17..20 -> {
                // Show evening image
                morningImage.visibility = View.GONE
                afternoonImage.visibility = View.GONE
                eveningImage.visibility = View.GONE
                nightImage.visibility = View.VISIBLE
            }
            else -> {
                // Show night image
                morningImage.visibility = View.GONE
                afternoonImage.visibility = View.GONE
                eveningImage.visibility = View.GONE
                nightImage.visibility = View.VISIBLE
            }
        }
        val dateTextView = rootView.findViewById<TextView>(R.id.date)
        val date = Calendar.getInstance().time
        val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
        val monthFormatter = SimpleDateFormat("MMMM", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("dd", Locale.getDefault())
        val dayOfWeek = dayFormatter.format(date)
        val month = monthFormatter.format(date)
        val dayOfMonth = dateFormatter.format(date)
        val formattedDate = "$dayOfWeek, $dayOfMonth $month "
        dateTextView.text = formattedDate




        return rootView
    }

    private fun getHolidayData(countryName: String) {



        // Get today's date
        calendarView.setOnDateChangeListener(CalendarView.OnDateChangeListener { view, year, month, dayOfMonth ->

            val todayDay = (dayOfMonth.toString())
            val todayYear = year
            val todayMonth = month + 1


        val url = "https://calendarific.com/api/v2/holidays?&api_key="+resources.getString(R.string.API)+"&country=" +countryName+ "&year="+todayYear+"&month="+todayMonth+"&day="+todayDay

        val request = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val jsonObjectResponse = jsonObject.getJSONObject("response")
                    holidayDataArray = jsonObjectResponse.getJSONArray("holidays")
                    holidayRecyclerView.adapter?.notifyDataSetChanged()



                } catch (e: Exception) {
                    e.printStackTrace()

                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()

            })

        Volley.newRequestQueue(requireContext()).add(request)

    })

}

    inner class HolidayAdapter : RecyclerView.Adapter<HolidayViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolidayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.test_layout, parent, false)

            return HolidayViewHolder(view)
        }

        override fun onBindViewHolder(holder: HolidayViewHolder, position: Int) {
            if (holidayDataArray.length() == 0) {
                holder.holidayName.text = "No Events"
                holder.holidayType.text = ""
            } else {
                try {
                    val holiday = holidayDataArray.getJSONObject(position)

                    val holidayObject = holidayDataArray.getJSONObject(position)
                    val holidayName = holidayObject.getString("name")
                    if (holidayName.isNullOrEmpty()) {
                        holder.holidayName.text = "No holiday Today"
                    } else {
                        holder.holidayName.text = holidayName
                    }
                    holder.holidayType.text = holidayObject.getString("primary_type")
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
        } }  override fun getItemCount(): Int {
                return if (holidayDataArray.length() == 0) 1 else holidayDataArray.length()
        }
    }

    inner class HolidayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val holidayName: TextView = itemView.findViewById(R.id.holidayName)
        val holidayType: TextView = itemView.findViewById(R.id.holidayType)
    }

}