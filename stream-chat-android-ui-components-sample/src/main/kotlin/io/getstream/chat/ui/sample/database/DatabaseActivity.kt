package io.getstream.chat.ui.sample.database

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.getstream.chat.ui.sample.R
import io.getstream.chat.ui.sample.databinding.ActivityDatabaseBinding

class DatabaseActivity : AppCompatActivity() {

    private val binding by lazy { ActivityDatabaseBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<DatabaseViewModel> {
        DatabaseViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.openBtn.setOnClickListener {
            viewModel.onOpen(Database.TEST_1)
        }

        binding.closeBtn.setOnClickListener {
            viewModel.onClose(Database.TEST_1)
        }

        binding.deleteBtn.setOnClickListener {
            viewModel.onDeleteClick(Database.TEST_1)
        }

        binding.generateBtn.setOnClickListener {
            viewModel.onGenerateClick(Database.TEST_1)
        }

        binding.readBtn.setOnClickListener {
            viewModel.onReadClick(Database.TEST_1)
        }

        binding.readCidsBtn.setOnClickListener {
            viewModel.onReadCidsClick(Database.TEST_1)
        }

        binding.readManyBtn.setOnClickListener {
            viewModel.onReadManyClick(Database.TEST_1)
        }

    }

}