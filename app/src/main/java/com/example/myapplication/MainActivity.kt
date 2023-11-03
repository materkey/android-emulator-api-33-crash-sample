package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.myapplication.shimmer.ShimmerFrameLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val categorySkeleton: ShimmerFrameLayout = findViewById(R.id.category_skeleton)
        categorySkeleton.visibility = View.VISIBLE
        categorySkeleton.startShimmer()
    }

}
