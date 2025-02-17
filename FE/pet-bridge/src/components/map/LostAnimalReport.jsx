import React, {useState, useEffect} from "react"
import Button from "components/common/Button"
import Editor from "components/common/Editor"
import {useNavigate} from "react-router-dom"
import {useSelector} from "react-redux"
import {selectId, selectImage, selectNickname} from "features/user/users-slice"
import {registArticle} from "api/boards-api"
import markerImg from "../../assets/image/marker.png"
import {Toast} from "utils/common-utils"
import Profile from "components/common/Profile"

const {kakao} = window

const LostAnimalReport = () => {
  const [title, setTitle] = useState("")
  const [editorContent, setEditorContent] = useState("")
  const [imageSrc, setImageSrc] = useState(null)
  const [imageFile, setImageFile] = useState(null)
  const [position, setPosition] = useState(null)
  const navigate = useNavigate()

  const currentUserImage = useSelector(selectImage)
  const currentUserNickname = useSelector(selectNickname)
  const currentUserId = useSelector(selectId)

  useEffect(() => {
    const container = document.getElementById("map")
    const defaultCenter = new kakao.maps.LatLng(36.355383, 127.298445)
    const options = {
      center: defaultCenter,
      level: 3,
    }

    const map = new kakao.maps.Map(container, options)

    const imageSize = new kakao.maps.Size(50, 50)
    const imageOption = {offset: new kakao.maps.Point(25, 50)}

    const markerImage = new kakao.maps.MarkerImage(
      markerImg,
      imageSize,
      imageOption
    )
    const marker = new kakao.maps.Marker({
      position: map.getCenter(),
      image: markerImage,
    })
    marker.setMap(map)

    kakao.maps.event.addListener(map, "click", function (mouseEvent) {
      const latlng = mouseEvent.latLng
      marker.setPosition(latlng)
      setPosition(latlng)
    })
  }, [])
  const [error, setError] = useState(null)

  // 파일 선택 시 호출되는 함수
  const handleFileChange = (event) => {
    const file = event.target.files[0]
    const maxSizeInBytes = 30 * 1024 * 1024 // 50MB 크기 제한
    if (file.size > maxSizeInBytes) {
      setError("파일 크기는 30MB를 초과할 수 없습니다.")
      return
    }
    if (file) {
      const url = URL.createObjectURL(file)
      setImageSrc(url)
      setImageFile(file)
    }
  }

  const writeArticle = async () => {
    if (editorContent.trim() === "") {
      Toast.fire({icon: "warning", title: "내용을 입력해주세요."})
      return
    }
    if (title.trim() === "") {
      Toast.fire({icon: "warning", title: "제목을 입력해주세요."})
      return
    }
    if (!imageSrc) {
      Toast.fire({icon: "warning", title: "대표 사진을 입력해주세요."})
      return
    }
    if (!position) {
      Toast.fire({icon: "warning", title: "지도를 클릭해서 위치를 선택하세요."})
      return
    }

    const newArticle = {
      title: title,
      type: "LOST",
      content: editorContent,
      lat: position.getLat(),
      lon: position.getLng(),
    }

    const formData = new FormData()
    formData.append(
      "boardRegistRequestDto",
      new Blob([JSON.stringify(newArticle)], {type: "application/json"})
    )
    if (imageFile) {
      formData.append("thumbnail", imageFile)
    }

    try {
      await registArticle(formData)
      Toast.fire({icon: "success", title: "글이 작성됐어요."})
      navigate(`/lost-and-found`)
    } catch (e) {
      console.error(e)
      Toast.fire({icon: "warning", title: "글 작성에 실패했어요"})
    }
  }

  const resetImage = () => {
    setImageSrc(null)
    setImageFile(null)
  }

  return (
    <div className="mx-auto flex w-full max-w-[1000px] flex-col rounded-lg bg-white p-6 shadow-md">
      {" "}
      {/* 1000px 고정 너비 설정 */}
      <button
        onClick={() => navigate(-1)}
        className="mb-4 text-left text-blue-500 hover:underline" // 좌측 정렬 설정
      >
        &larr; 돌아가기
      </button>
      <input
        className="mb-4 w-full rounded-lg border border-gray-300 p-4 text-center text-2xl font-bold placeholder:text-gray-500" // 가운데 정렬 설정
        placeholder="제목을 입력하세요"
        onChange={(e) => setTitle(e.target.value)}
        value={title}
      />
      <hr className="my-4" />
      <Profile
        nickname={currentUserNickname}
        image={currentUserImage}
        userId={currentUserId}
        isMe={true}
      />
      <hr className="my-4" />
      <div className="mb-4">
        <h2 className="mb-2 text-xl font-semibold">대표사진</h2>
        {imageSrc ? (
          <div className="relative">
            <img
              src={imageSrc}
              alt="Uploaded Preview"
              className="max-h-96 w-full rounded border object-contain"
            />
            <button
              onClick={resetImage}
              className="absolute right-2 top-2 rounded-full border bg-white p-1 shadow-lg hover:bg-gray-100"
            >
              ✖
            </button>
          </div>
        ) : (
          <div className="flex h-64 w-full items-center justify-center rounded-lg border border-dashed border-gray-300 bg-gray-50">
            <p className="text-gray-500">대표사진을 입력해주세요.</p>
          </div>
        )}
        <div className="mt-2">
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            className="block w-full cursor-pointer rounded-lg border border-gray-300 bg-white text-sm text-gray-900 focus:outline-none"
          />

          {error && <p className="text-sm text-red-500">{error}</p>}
        </div>
      </div>
      <div id="map" className="mb-4 h-[450px] w-full rounded border"></div>
      <div className="mb-8 min-h-72 w-full">
        {" "}
        <Editor value={editorContent} onChange={setEditorContent} />
      </div>
      <div className="mt-8 flex justify-end space-x-2">
        {" "}
        {/* 여백 추가 및 배치 조정 */}
        <Button text={"작성하기"} onClick={writeArticle} />
        <Button text={"삭제하기"} onClick={() => navigate(-1)} />
      </div>
    </div>
  )
}

export default LostAnimalReport
