package com.example.sanjeevani

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.TimeUnit

class PlaceAutocompleteAdapter(context: Context, private val placesClient: PlacesClient) :
    ArrayAdapter<AutocompletePrediction>(context, android.R.layout.simple_list_item_1),
    Filterable {

    private var resultList: List<AutocompletePrediction> = arrayListOf()
    private var biasLocation: LatLng? = null

    fun setBiasLocation(latLng: LatLng) {
        this.biasLocation = latLng
    }

    override fun getCount(): Int = resultList.size

    override fun getItem(position: Int): AutocompletePrediction? {
        return if (position < resultList.size) resultList[position] else null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            android.R.layout.simple_list_item_1, parent, false
        )
        val item = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = item?.getFullText(null)
        textView.setTextColor(android.graphics.Color.BLACK) // Ensure text is visible
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (!constraint.isNullOrEmpty()) {
                    val requestBuilder = FindAutocompletePredictionsRequest.builder()
                        .setQuery(constraint.toString())
                        .setTypesFilter(listOf("hospital")) // Specifically search for hospitals

                    biasLocation?.let {
                        val radiusDegrees = 0.1 // Approx 10km
                        val bounds = RectangularBounds.newInstance(
                            LatLng(it.latitude - radiusDegrees, it.longitude - radiusDegrees),
                            LatLng(it.latitude + radiusDegrees, it.longitude + radiusDegrees)
                        )
                        requestBuilder.setLocationBias(bounds)
                    }

                    val request = requestBuilder.build()

                    try {
                        val task = placesClient.findAutocompletePredictions(request)
                        // Wait for results (blocking the filter thread is allowed)
                        val response = Tasks.await(task, 5, TimeUnit.SECONDS)
                        val predictions = response.autocompletePredictions
                        
                        filterResults.values = predictions
                        filterResults.count = predictions.size
                        Log.d("PlaceAdapter", "Found ${predictions.size} results for: $constraint")
                    } catch (e: Exception) {
                        Log.e("PlaceAdapter", "Error fetching predictions: ${e.message}")
                    }
                }
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    resultList = results.values as List<AutocompletePrediction>
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}
