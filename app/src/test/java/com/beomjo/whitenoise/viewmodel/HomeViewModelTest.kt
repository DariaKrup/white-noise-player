package com.beomjo.whitenoise.viewmodel

import androidx.lifecycle.Observer
import com.beomjo.compilation.util.Event
import com.beomjo.whitenoise.BaseTest
import com.beomjo.whitenoise.model.Category
import com.beomjo.whitenoise.model.User
import com.beomjo.whitenoise.repositories.auth.AuthRepository
import com.beomjo.whitenoise.repositories.home.HomeRepository
import com.beomjo.whitenoise.ui.main.home.HomeViewModel
import com.google.firebase.FirebaseException
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Test

class HomeViewModelTest : BaseTest() {

    @MockK
    lateinit var authRepository: AuthRepository

    @MockK
    lateinit var homeRepository: HomeRepository

    private lateinit var viewModel: HomeViewModel

    override fun onBefore() {
        viewModel = spyk(
            HomeViewModel(authRepository, homeRepository),
            recordPrivateCalls = true,
        )
    }

    @Test
    fun `init() 호출`() {
        //given
        justRun { viewModel invokeNoArgs "loadUserInfo" }
        justRun { viewModel invokeNoArgs "loadHomeCategoryList" }

        //when
        viewModel.init()

        //then
        verify { viewModel invokeNoArgs "loadUserInfo" }
        verify { viewModel invokeNoArgs "loadHomeCategoryList" }
    }

    @Test
    fun `유저 정보 로드 성공`() {
        //given
        val mockUser = mockk<User>()
        every { authRepository.getUserInfo() } returns mockUser
        val userObserver = mockk<Observer<User>> {
            every { onChanged(mockUser) } just Runs
        }
        viewModel.user.observeForever(userObserver)

        //when
        viewModel invokeNoArgs "loadUserInfo"

        //then
        verify { authRepository.getUserInfo() }
        verify { userObserver.onChanged(eq(mockUser)) }
    }

    @Test
    fun `유저 정보 로드 실패`() {
        //given
        val exception = mockk<FirebaseException>()
        val errorMsg = "Fail"
        every { exception.message } returns errorMsg
        every { authRepository.getUserInfo() } throws exception
        val userObserver = mockk<Observer<User>>()
        val toastObserver = mockk<Observer<Event<String>>> {
            every { onChanged(Event(errorMsg)) } just Runs
        }
        viewModel.user.observeForever(userObserver)
        viewModel.toast.observeForever(toastObserver)

        //when
        viewModel invokeNoArgs "loadUserInfo"

        //then
        verify { authRepository.getUserInfo() }
        verify { userObserver wasNot Called }
        verify { toastObserver.onChanged(eq(Event(errorMsg))) }
    }

    @Test
    fun `홈 카테고리 데이터 로드 성공`() {
        //given
        val homeCategory = mockk<Category>()
        val homeCategories = listOf(homeCategory)
        coEvery { homeRepository.getHomeCategoryList() } returns homeCategories
        val refreshObserver = mockk<Observer<Boolean>> {
            every { onChanged(false) } just Runs
        }
        viewModel.isRefreshing.observeForever(refreshObserver)

        //when
        viewModel invokeNoArgs "loadHomeCategoryList"

        //then
        coVerify { homeRepository.getHomeCategoryList() }
        verify { refreshObserver.onChanged(eq(false)) }
    }

    @Test
    fun `홈 카테고리 데이터 로드 실패`() {
        //given
        val exception = mockk<FirebaseException>()
        val errorMsg = "Fail"
        every { exception.message } returns errorMsg
        coEvery { homeRepository.getHomeCategoryList() } throws exception
        val refreshObserver = mockk<Observer<Boolean>> {
            every { onChanged(false) } just Runs
        }
        val toastObserver = mockk<Observer<Event<String>>> {
            every { onChanged(Event(errorMsg)) } just Runs
        }
        viewModel.isRefreshing.observeForever(refreshObserver)
        viewModel.toast.observeForever(toastObserver)

        //when
        viewModel invokeNoArgs "loadHomeCategoryList"

        //then
        coVerifyOrder { homeRepository.getHomeCategoryList() }
        verify { toastObserver.onChanged(eq(Event(errorMsg))) }
        verify { refreshObserver.onChanged(eq(false)) }
    }

    @Test
    fun `Refresh 하여 재로딩`() {
        //given
        justRun { viewModel invokeNoArgs "loadHomeCategoryList" }
        val refreshObserver = mockk<Observer<Boolean>> {
            every { onChanged(true) } just Runs
        }
        viewModel.isRefreshing.observeForever(refreshObserver)

        //when
        viewModel.onRefresh()

        //then
        verify { refreshObserver.onChanged(eq(true)) }
        verify { viewModel invokeNoArgs "loadHomeCategoryList" }
    }
}